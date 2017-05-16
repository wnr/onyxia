(ns onyxia.react
  (:require [cljsjs.react.dom]
            [onyxia.react-utils :refer [map-to-react-attributes
                                        add-key-attribute]]
            [onyxia.vdom :as vdom]
            [ysera.error :refer [error]]
            [onyxia.dom-operator :refer [add-pending-operation!]]))

(declare create-react-element)
(declare render!)

(def component-cache (atom {}))

(defn- ensure-global-react!
  []
  (when (not js/React)
    (error "No global React instance found."))
  (when (not js/ReactDOM)
    (error "No global ReactDOM instance found.")))

(defn- react-instance->parent-element
  [react-instance]
  (let [internal-instance (aget react-instance "_reactInternalInstance")
        host-parent (aget internal-instance "_hostParent")]
    (or (and host-parent (js/ReactDOM.findDOMNode (aget host-parent "_hostNode")))
        (aget internal-instance "_hostContainerInfo" "_node"))))

(defn- get-input-definition
  [input-definitions name]
  (let [input-definition (get input-definitions name)]
    (when (not input-definition)
      (error "No input-definition found for input " name))
    input-definition))

(defn- get-output-definition
  [output-definitions name]
  (let [output-definition (get output-definitions name)]
    (when (not output-definition)
      (error "No output-definition found for output " name))
    output-definition))

(defn- get-ancestor-view-instance-atom
  [{ancestor-definition :ancestor-definition
    ancestor-views-data :ancestor-views-data}]
  (let [view-data (get ancestor-views-data ancestor-definition)]
    (when (not view-data)
      (error "Unable to find ancestor view data for ancestor definition" (:name ancestor-definition)))
    (:view-state-atom view-data)))

(defn init-input-systems!
  "docstring"
  [{component         :component
    definition        :definition
    on-state-changed  :on-state-changed
    input-definitions :input-definitions}]
  {:pre [component definition on-state-changed input-definitions]}
  (set! (.-input component)
        (reduce-kv (fn [input-state input-name input]
                     (assoc input-state
                       input-name
                       {:instance ((:get-instance (get-input-definition input-definitions (:name input)))
                                    (merge (dissoc input :name)
                                           {:on-state-changed on-state-changed}))}))
                   {}
                   (:input definition))))

(defn- get-input-system-instances-data
  [{component :component}]
  {:pre [component]}
  (.-input component))

(defn- get-input-system-instances
  [{component :component}]
  {:pre [component]}
  (->> (get-input-system-instances-data {:component component})
       (vals)
       (map :instance)))

(defn all-input-system-instances-ready?
  [{component :component}]
  {:pre [component]}
  (let [input-instances (get-input-system-instances {:component component})]
    (if (empty? input-instances)
      true
      (->> input-instances
           (some (fn [input-instance]
                   (not ((:ready? input-instance)))))
           (not)))))

(defn init-view-state!
  [{component  :component
    definition :definition}]
  (set! (.-view-state-atom component) (atom (when (:get-initial-state definition)
                                              ((:get-initial-state definition))))))

(defn get-view-state-atom
  [component]
  (.-view-state-atom component))

(defn- get-view-input
  [component]
  (reduce-kv (fn [view-input input-name {instance :instance}]
               (assoc view-input
                 input-name
                 ((:get-value instance))))
             (merge (aget component "props" "input")
                    {:view-state (deref (.-view-state-atom component))})
             (get-input-system-instances-data {:component component})))

(defn- should-render? [component definition]
  (let [should-render?-fn (:should-render? definition)]
    (if should-render?-fn
      (should-render?-fn (get-view-input component))
      true)))

(defn- did-render!
  [{component  :component
    definition :definition}]
  (when-let [did-render-fn (:did-render definition)]
    ;; TODO: Not nice to merge like this. What if some input to this view is called "element"?
    (did-render-fn (merge {:element                (react-instance->parent-element component)
                           :add-pending-operation! add-pending-operation!}
                          (get-view-input component)))))

(defn did-mount!
  [{component :component}]
  ;; TODO: Should probably be signaled to view as well.
  (doseq [input-instance (get-input-system-instances {:component component})]
    (when (:did-mount input-instance)
      (add-pending-operation! ((:did-mount input-instance)
                                {;; TODO: Should this be the root element of the view instead?
                                 :element (react-instance->parent-element component)})))))

(defn will-unmount!
  [{component :component}]
  ;; TODO: Should probably be signaled to view as well.
  (doseq [input-instances (get-input-system-instances {:component component})]
    (when (:will-unmount input-instances)
      ;; Not adding to a pending operation here, because we do not have control of React.
      ;; We want to call the unmount signal before the component is unmounted.
      ((:will-unmount input-instances)
        {;; TODO: Should this be the root element of the view instead?
         :element (react-instance->parent-element component)}))))

(defn create-view-component
  [{definition          :definition
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ancestor-views-data :ancestor-views-data}]
  (js/React.createClass (clj->js {:displayName           (:name definition)
                                  :componentWillMount    (fn []
                                                           (this-as component
                                                             (init-view-state! {:component component :definition definition})
                                                             (init-input-systems! {:component component :definition definition :input-definitions input-definitions :on-state-changed (fn [] (.onStateChanged component))})
                                                             (add-watch (get-view-state-atom component) :renderer (fn [_ _ _ _] (.onStateChanged component))))
                                                           nil)
                                  :componentDidMount     (fn []
                                                           (this-as component
                                                             (did-mount! {:component component})
                                                             (did-render! {:component component :definition definition}))
                                                           nil)
                                  :render                (fn []
                                                           (this-as component
                                                             (when (all-input-system-instances-ready? {:component component})
                                                               (let [view-state-atom (get-view-state-atom component)]
                                                                 (create-react-element ((:render definition)
                                                                                         (get-view-input component))
                                                                                       {:on-dom-event        (fn [{handlers :handlers}]
                                                                                                               (doseq [handler handlers]
                                                                                                                 (if (sequential? handler)
                                                                                                                   ;; A function identifier is to be invoked.
                                                                                                                   (let [event-handling-definition (or (when (map? (first handler))
                                                                                                                                                         (first handler))
                                                                                                                                                       definition)
                                                                                                                         handle-fn-key (if (map? (first handler))
                                                                                                                                         (second handler)
                                                                                                                                         (first handler))
                                                                                                                         handle-fn (get (:events event-handling-definition) handle-fn-key)
                                                                                                                         handle-fn-data (last handler)]
                                                                                                                     (if (not handle-fn)
                                                                                                                       (throw (js/Error (str "Cannot find " handle-fn-key " function in definition " (:name event-handling-definition))))
                                                                                                                       (let [handling-view-state-atom (if (not= event-handling-definition definition)
                                                                                                                                                        (get-ancestor-view-instance-atom {:ancestor-definition event-handling-definition
                                                                                                                                                                                          :ancestor-views-data ancestor-views-data})
                                                                                                                                                        view-state-atom)]
                                                                                                                         ;; TODO: Could avoid multiple swaps by merging all handler functions into a single composite transform.
                                                                                                                         (swap! handling-view-state-atom (fn [state]
                                                                                                                                                           (if-let [result (handle-fn state handle-fn-data)]
                                                                                                                                                             result
                                                                                                                                                             state))))))
                                                                                                                   ;; A raw function has been passed as handler, simply invoke it.
                                                                                                                   (handler))))
                                                                                        :input-definitions   input-definitions
                                                                                        :output-definitions  output-definitions
                                                                                        :ancestor-views-data (assoc ancestor-views-data definition {:view-state-atom view-state-atom})
                                                                                        :component           component})))))
                                  :shouldComponentUpdate (fn []
                                                           (this-as component
                                                             (should-render? component definition)))
                                  :componentDidUpdate    (fn []
                                                           (this-as component
                                                             (did-render! {:component component :definition definition}))
                                                           nil)
                                  :componentWillUnmount  (fn []
                                                           (this-as component
                                                             (will-unmount! {:component component})
                                                             nil)
                                                           nil)
                                  :onStateChanged        (fn []
                                                           (this-as component
                                                             (let [view-state-atom (get-view-state-atom component)]
                                                               (when (should-render? component definition)
                                                                 (reduce (fn [_ output]
                                                                           (let [output-definition (get-output-definition output-definitions (:name output))]
                                                                             ((:handle! output-definition)
                                                                               {:view-output         output
                                                                                :view-state          (get-view-input component)
                                                                                :render!             render!
                                                                                :input-definitions   input-definitions
                                                                                :output-definitions  output-definitions
                                                                                :ancestor-views-data (assoc ancestor-views-data definition {:view-state-atom view-state-atom})})))
                                                                         nil
                                                                         (:output definition))
                                                                 (.setState component {:view-input (get-view-input component)}))))
                                                           nil)})))

(defn- definition->component
  [{definition :definition
    :as        arguments}]
  ;; Currently not caring about whether input-definitions have changed.
  (let [cached-component (get @component-cache definition)]
    (if cached-component
      cached-component
      (let [component (create-view-component arguments)]
        (swap! component-cache assoc definition component)
        component))))

(defn modify-attributes
  [attributes {component :component}]
  (reduce (fn [attributes input-instance]
            (if-let [element-attribute-modifier (:element-attribute-modifier input-instance)]
              (do
                (if-let [modified-attributes (element-attribute-modifier {:attributes attributes})]
                  modified-attributes
                  attributes))
              attributes))
          attributes
          (get-input-system-instances {:component component})))

(defn- create-react-element
  [vdom-element {on-dom-event        :on-dom-event
                 input-definitions   :input-definitions
                 output-definitions  :output-definitions
                 ancestor-views-data :ancestor-views-data   ;; Optional. Needed if function locators are to be sent from a view to another.
                 component           :component             ;; Optional. Needed to activate view-specific input systems. If not present, only standard HTML attributes and such will be processed.
                 :as                 system-options}]
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

    ;; A view (tree structure of html elements with a lifecycle) is to be rendered.
    (= (first vdom-element) :view)
    (let [attributes (second vdom-element)
          definition (:definition attributes)
          children (nthrest vdom-element 2)
          input (merge (or (:input attributes) {})
                       (when children
                         ;; TODO: What about conflicts with existing input called "children"?
                         {:children children}))]
      (when-not definition
        (cond
          (contains? attributes :definition)
          (error (str "Unable to find view definition. " vdom-element))

          :else
          (error (str "The view must contain a :definition key. Did you spell it wrong? " vdom-element))))
      (js/React.createElement (definition->component {:definition          definition
                                                      :input-definitions   input-definitions
                                                      :output-definitions  output-definitions
                                                      :ancestor-views-data ancestor-views-data})
                              #js{:input input}))

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [vdom-element (vdom/formalize-element vdom-element)
          attributes (second vdom-element)
          children (nth vdom-element 2)
          react-element-args (concat [(name (first vdom-element))
                                      (clj->js (-> (if component
                                                     (modify-attributes attributes {:component component})
                                                     attributes)
                                                   (map-to-react-attributes {:on-dom-event on-dom-event})))]
                                     (clj->js (if (and (= (count children) 1)
                                                       (or (string? (first children))
                                                           (number? (first children))))
                                                [(first children)]
                                                (map (fn [child]
                                                       (create-react-element child system-options))
                                                     children))))]
      (apply js/React.createElement react-element-args))

    ;; A sequence of elements (that will need to be handled as such for React with keys etc).
    (vdom/element-sequence? vdom-element)
    (map-indexed (fn [index node]
                   (let [node (if (vdom/element? node)
                                (add-key-attribute (vdom/ensure-attributes-map node) (str (hash node) "-" index))
                                node)]
                     (create-react-element node system-options)))
                 (vdom/clean-element-sequence vdom-element))

    :default
    (throw (js/Error (str "Unknown vdom element: " vdom-element)))))

(defn render!
  [{view                :view
    target-element      :target-element
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ;; Optional. May be used to define conceptual parents of the rendered view tree (even though it is not a parent HTML-wise).
    ancestor-views-data :ancestor-views-data}]
  (ensure-global-react!)
  (if (nil? view)
    (js/ReactDOM.unmountComponentAtNode target-element)
    (let [react-element (create-react-element view {:input-definitions   input-definitions
                                                    :output-definitions  output-definitions
                                                    :on-dom-event        (fn [_])
                                                    :ancestor-views-data ancestor-views-data})]
      (js/ReactDOM.render react-element target-element))))
