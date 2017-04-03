(ns onyxia.dom-operator
  (:require [cljsjs.react.dom]
            [onyxia.vdom :as vdom]
            [onyxia.view-definitions :as view-definitions]
            [onyxia.input-definitions :as input-definitions]
            [onyxia.output-definitions :as output-definitions]
            [ysera.error :refer [error]]))

(def pending-operations-atom (atom {:status    :idle
                                    :read-dom  []
                                    :write-dom []}))

(defn- execute-operations-seq! [seq]
  (reduce (fn [_ operation]
            ((:execute! operation)))
          nil
          seq))

(defn execute-pending-operations! []
  (let [pending-operations @pending-operations-atom]
    (reset! pending-operations-atom {:status    :idle
                                     :read-dom  []
                                     :write-dom []})
    (let [read-operations  (:read-dom pending-operations)
          write-operations (:write-dom pending-operations)]
      (execute-operations-seq! read-operations)
      (execute-operations-seq! write-operations))))
(defn queue-pending-operations-execution! []
  (when (not= (:status @pending-operations-atom) :idle)
    (error "Cannot queue unless status idle."))
  (swap! pending-operations-atom assoc :status :execution-queued)
  (js/requestAnimationFrame (fn []
                              (execute-pending-operations!))))

(defn add-pending-operation! [operation]
  (let [status (:status (swap! pending-operations-atom (fn [pending-operations]
                                                         (update pending-operations (:operation operation) conj operation))))]
    (when (= status :idle)
      (queue-pending-operations-execution!))))

(defn ensure-global-react!
  []
  (when (not js/React)
    (error "No global React instance found."))
  (when (not js/ReactDOM)
    (error "No global ReactDOM instance found.")))

(declare create-react-element)

(defn react-instance->parent-element
  [react-instance]
  (or (js/ReactDOM.findDOMNode react-instance)
      (let [internal-instance (aget react-instance "_reactInternalInstance")
            host-parent (aget internal-instance "_hostParent")]
        (or (and host-parent (js/ReactDOM.findDOMNode (aget host-parent "_hostNode")))
            (aget internal-instance "_hostContainerInfo" "_node")))))

(def react-view-component
  ((fn []
     (js/React.createClass (clj->js {:displayName          "View"
                                     :componentWillMount   (fn []
                                                             ; (println "componentWillMount")
                                                             (this-as this
                                                                      (let [definition (aget this "props" "definition")]
                                                                        (if (:input definition)
                                                                          (set! (.-input this)
                                                                                (reduce-kv (fn [input-state input-name input]
                                                                                             (assoc input-state
                                                                                                    input-name
                                                                                                    {:instance ((:get-instance (input-definitions/get! (:name input)))
                                                                                                                (merge (dissoc input :name)
                                                                                                                       {:on-state-changed (fn []
                                                                                                                                            (.onStateChanged this))}))}))
                                                                                           {}
                                                                                           (:input definition))))
                                                                        (let [view-state-atom (atom (when (:get-initial-state definition)
                                                                                                      ((:get-initial-state definition))))]
                                                                          (add-watch view-state-atom :renderer (fn [_ _ _ _] (.onStateChanged this)))
                                                                          (set! (.-view-state-atom this) view-state-atom)))))
                                     :componentDidMount    (fn []
                                                             (this-as this
                                                                      (let [definition (aget this "props" "definition")
                                                                             ;; TODO: Should this be the root element of the view instead?
                                                                             parent-element (react-instance->parent-element this)]
                                                                        ; (println (:name definition) "componentDidMount")
                                                                        (reduce-kv (fn [input-state input-name {input :instance}]
                                                                                     (when (:componentDidMount input)
                                                                                       (add-pending-operation! ((:componentDidMount input)
                                                                                                                {:element parent-element}))))
                                                                                   {}
                                                                                   (.-input this))
                                                                        (when-let [did-render-fn (:did-render definition)]
                                                                          (did-render-fn (merge {:element parent-element} ;; TODO: Not nice to merge like this. What if some input to this view is called "element"?
                                                                                                ((.-getViewInput this))))))
                                                               nil))
                                     :render               (fn []
                                                             (this-as this
                                                               (let [definition      (aget this "props" "definition")
                                                                     view-state-atom (.-view-state-atom this)]
                                                                 ; (println (:name definition) "render" @view-state-atom)
                                                                 (when-not (->> (.-input this)
                                                                                (vals)
                                                                                (some (fn [input]
                                                                                        (not ((:ready? (:instance input)))))))
                                                                   (create-react-element ((:render definition)
                                                                                          ((.-getViewInput this)))
                                                                                         {:on-dom-event (fn [{type :type
                                                                                                              data :data}]
                                                                                                          (let [handle-fn ((first data) (:events definition))]
                                                                                                            (if (not handle-fn)
                                                                                                              (throw (js/Error (str "Cannot find " (first data) " function in definition " (:name definition))))
                                                                                                              (swap! view-state-atom handle-fn (second data)))))})))))
                                     :componentDidUpdate (fn []
                                                           (this-as this
                                                                    (let [definition (aget this "props" "definition")
                                                                          parent-element (react-instance->parent-element this)]
                                                                      (when-let [did-render-fn (:did-render definition)]
                                                                        (did-render-fn (merge {:element parent-element} ;; TODO: Not nice to merge like this. What if some input to this view is called "element"?
                                                                                              ((.-getViewInput this))))))))
                                     :componentWillUnmount (fn []
                                                             ;(println "componentWillUnmount")
                                                             )
                                     :getViewInput         (fn []
                                                             (this-as this
                                                               (reduce-kv (fn [view-input input-name {instance :instance}]
                                                                            (assoc view-input
                                                                                   input-name
                                                                                   ((:get-value instance))))
                                                                          (merge (aget this "props" "input")
                                                                                 {:view-state (deref (.-view-state-atom this))})
                                                                          (.-input this))))
                                     :onStateChanged       (fn []
                                                             (this-as this
                                                               (let [definition      (aget this "props" "definition")]
                                                                 (reduce (fn [_ output]
                                                                           (let [output-definition (output-definitions/get! (:name output))]
                                                                             ((:handle! output-definition) output ((.-getViewInput this)))))
                                                                         nil
                                                                         (:output definition))
                                                                 (.forceUpdate this))))})))))

(defn create-react-element [vdom-element {on-dom-event :on-dom-event}]
  (ensure-global-react!)
  (cond
    (string? vdom-element)
    vdom-element

    (number? vdom-element)
    (str vdom-element)

    ;; A view (collection of html element with a lifecycle) is to be rendered.
    (= (first vdom-element) :view)
    (let [attributes (second vdom-element)
          name       (:name attributes)
          input      (or (:input attributes) {})
          definition (view-definitions/get! name)]
      (when-not definition
        (error "Unable to find view definition with name " name))
      (js/React.createElement react-view-component (js-obj "definition" definition "input" input)))

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [vdom-element       (vdom/formalize-element vdom-element)
          children           (nth vdom-element 2)
          react-element-args (concat [(name (first vdom-element))
                                      (clj->js (vdom/map-to-react-attributes (second vdom-element) {:on-dom-event on-dom-event}))]
                                     (clj->js (if (and (= (count children) 1)
                                                       (or (string? (first children))
                                                           (number? (first children))))
                                                [(first children)]
                                                (map (fn [child] (create-react-element child {:on-dom-event on-dom-event})) children))))]
      (apply js/React.createElement react-element-args))

    :default
    (throw (js/Error (str "Unknown vdom element: " vdom-element)))))

(defn render! [view target]
  (ensure-global-react!)
  (if (nil? view)
    (js/ReactDOM.unmountComponentAtNode target)
    (let [react-element (create-react-element view {:on-dom-event (fn [_])})]
      (js/ReactDOM.render react-element target))))
