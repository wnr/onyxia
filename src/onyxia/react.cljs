(ns onyxia.react
  (:require [cljsjs.react.dom]
            [onyxia.react-utils :refer [map-to-react-attributes]]
            [onyxia.vdom :as vdom]
            [ysera.error :refer [error]]
            [onyxia.view-definitions :as view-definitions]
            [onyxia.input-definitions :as input-definitions]
            [onyxia.output-definitions :as output-definitions]
            [onyxia.dom-operator :refer [add-pending-operation!]]))

(def component-cache (atom {}))

(defn- ensure-global-react!
  []
  (when (not js/React)
    (error "No global React instance found."))
  (when (not js/ReactDOM)
    (error "No global ReactDOM instance found.")))

(declare create-react-element)

(defn- react-instance->parent-element
  [react-instance]
  (or (js/ReactDOM.findDOMNode react-instance)
      (let [internal-instance (aget react-instance "_reactInternalInstance")
            host-parent (aget internal-instance "_hostParent")]
        (or (and host-parent (js/ReactDOM.findDOMNode (aget host-parent "_hostNode")))
            (aget internal-instance "_hostContainerInfo" "_node")))))

(defn- get-view-input
  [component]
  (reduce-kv (fn [view-input input-name {instance :instance}]
               (assoc view-input
                      input-name
                      ((:get-value instance))))
             (merge (aget component "props" "input")
                    {:view-state (deref (.-view-state-atom component))})
             (.-input component)))

(defn- did-render! [component definition]
  (when-let [did-render-fn (:did-render definition)]
    ;; TODO: Not nice to merge like this. What if some input to this view is called "element"?
    (did-render-fn (merge {:element (react-instance->parent-element component)
                           :add-pending-operation! add-pending-operation!}
                          (get-view-input component)))))

(defn- should-render? [component definition]
  (let [should-render?-fn (:should-render? definition)]
    (if should-render?-fn
      (should-render?-fn (merge (get-view-input component)))
      true)))

(defn create-view-component
  [definition]
  (js/React.createClass (clj->js {:displayName          (:name definition)
                                  :componentWillMount   (fn []
                                                          (let [view-state-atom (atom (when (:get-initial-state definition)
                                                                                        ((:get-initial-state definition))))]
                                                            (this-as this
                                                                     (set! (.-view-state-atom this) view-state-atom)
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
                                                                     (add-watch view-state-atom :renderer (fn [_ _ _ _] (.onStateChanged this))))))
                                  :componentDidMount    (fn []
                                                          (this-as this
                                                                   (let [;; TODO: Should this be the root element of the view instead?
                                                                         parent-element (react-instance->parent-element this)]
                                                                     (reduce-kv (fn [input-state input-name {input :instance}]
                                                                                  (when (:did-mount input)
                                                                                    (add-pending-operation! ((:did-mount input)
                                                                                                             {:element parent-element}))))
                                                                                {}
                                                                                (.-input this))
                                                                     (did-render! this definition))
                                                            nil))
                                  :render               (fn []
                                                          (this-as this
                                                            (let [view-state-atom (.-view-state-atom this)]
                                                              (when-not (->> (.-input this)
                                                                             (vals)
                                                                             (some (fn [input]
                                                                                     (not ((:ready? (:instance input)))))))
                                                                (create-react-element ((:render definition)
                                                                                       (get-view-input this))
                                                                                      {:on-dom-event (fn [{type :type
                                                                                                           data :data}]
                                                                                                       (let [handle-fn ((first data) (:events definition))]
                                                                                                         (if (not handle-fn)
                                                                                                           (throw (js/Error (str "Cannot find " (first data) " function in definition " (:name definition))))
                                                                                                           (swap! view-state-atom handle-fn (second data)))))})))))
                                  :shouldComponentUpdate (fn []
                                                           (this-as this
                                                                    (should-render? this definition)))
                                  :componentDidUpdate (fn []
                                                        (this-as this
                                                                 (did-render! this definition)))
                                  :componentWillUnmount (fn []
                                                          (this-as this
                                                                   (let [;; TODO: Should this be the root element of the view instead?
                                                                         parent-element (react-instance->parent-element this)]
                                                                     (reduce-kv (fn [input-state input-name {input :instance}]
                                                                                  (when (:will-unmount input)
                                                                                    ((:will-unmount input)
                                                                                     {:element parent-element})))
                                                                                {}
                                                                                (.-input this)))
                                                            nil))
                                  :onStateChanged       (fn []
                                                          (this-as this
                                                                   (reduce (fn [_ output]
                                                                             (let [output-definition (output-definitions/get! (:name output))]
                                                                               ((:handle! output-definition) output (get-view-input this))))
                                                                           nil
                                                                           (:output definition))
                                                                   (when (should-render? this definition)
                                                                     (.forceUpdate this))))})))

(defn- definition->component
  [definition]
  (let [cached-component (get @component-cache definition)]
    (if cached-component
      cached-component
      (let [component (create-view-component definition)]
        (swap! component-cache assoc definition component)
        component))))

(defn- create-react-element [vdom-element {on-dom-event :on-dom-event}]
  (ensure-global-react!)
  (cond
    ;; React.createElement cares about children ordering, so it is important to keep nil children.
    ;; Otherwise, React won't be able to recognize unaffected children (so it will re-mount all of them).
    (nil? vdom-element)
    nil

    (string? vdom-element)
    vdom-element

    (number? vdom-element)
    (str vdom-element)

    ;; A view (collection of html element with a lifecycle) is to be rendered.
    (= (first vdom-element) :view)
    (let [attributes (second vdom-element)
          definition (or (:definition attributes) (view-definitions/get! (:name attributes)))
          input      (or (:input attributes) {})]
      (when-not definition
        (error (str "Unable to find view definition, " vdom-element)))
      (js/React.createElement (definition->component definition)
                              #js{:input input}))

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [vdom-element       (vdom/formalize-element vdom-element)
          children           (nth vdom-element 2)
          react-element-args (concat [(name (first vdom-element))
                                      (clj->js (map-to-react-attributes (second vdom-element) {:on-dom-event on-dom-event}))]
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
