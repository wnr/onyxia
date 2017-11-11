(ns onyxia.engine.inferno
  (:require
    [cljsjs.inferno]
    [cljsjs.inferno.create-element]
    [cljsjs.inferno.component]
    [cljsjs.inferno.create-class]
    [onyxia.engine.inferno-utils :refer [map-to-inferno-attributes]]
    [onyxia.vdom :as vdom]
    [ysera.error :refer [error]]
    [onyxia.view-instance :as vi]
    [onyxia.dom-operator :refer [add-pending-operation!]]))

(declare create-inferno-element)
(declare render!)

(def component-cache (atom {}))

(defn- ensure-global-inferno!
  []
  (when (not js/Inferno)
    (error "No global Inferno instance found."))
  (when (not js/Inferno.createElement)
    (error "Inferno.createElement needs to be present."))
  (when (not js/Inferno.createClass)
    (error "Inferno.createClass needs to be present.")))

(defn- component->parent-element
  [component]
  (let []
    (aget component "_vNode" "dom" "parentElement")))

(defn create-view-component
  [{definition          :definition
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ancestor-views-data :ancestor-views-data
    :as                 args}]
  (js/Inferno.createClass (clj->js {:displayName               (:name definition)
                                    :componentWillMount        (fn []
                                                                 (this-as component
                                                                   (let [view-instance (vi/create-view-instance (merge args {:render! render!}))]
                                                                     (aset component "viewInstance" view-instance)
                                                                     (vi/will-mount! view-instance {:parent-input     (aget component "props" "input")
                                                                                                    :on-state-changed (fn []
                                                                                                                        ((aget component "onStateChanged")))})))
                                                                 nil)
                                    :componentDidMount         (fn []
                                                                 (this-as component
                                                                   (vi/did-mount! (aget component "viewInstance") {:parent-element (component->parent-element component)}))
                                                                 nil)
                                    :render                    (fn []
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (when (vi/all-input-system-instances-ready? view-instance)
                                                                       (create-inferno-element (vi/render! view-instance)
                                                                                               {:on-dom-event        (fn [data]
                                                                                                                       (vi/handle-dom-event view-instance data))
                                                                                                :input-definitions   input-definitions
                                                                                                :output-definitions  output-definitions
                                                                                                :ancestor-views-data (assoc ancestor-views-data definition {:view-state-atom (vi/get-view-state-atom view-instance)})
                                                                                                :view-instance       view-instance})))))
                                    :componentWillReceiveProps (fn [next-props]
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (vi/set-parent-input! view-instance (aget next-props "input"))))
                                                                 nil)
                                    :shouldComponentUpdate     (fn []
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (vi/should-render? view-instance))))
                                    :componentDidUpdate        (fn []
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (vi/did-render! view-instance)))
                                                                 nil)
                                    :componentWillUnmount      (fn []
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (vi/will-unmount! view-instance)))
                                                                 nil)
                                    :onStateChanged            (fn []
                                                                 (this-as component
                                                                   (let [view-instance (aget component "viewInstance")]
                                                                     (.setState component {:view-input (vi/get-view-input view-instance)})))
                                                                 nil)})))

(defn- get-component
  [arguments]
  (let [cached-component (get @component-cache arguments)]
    (if cached-component
      cached-component
      (let [component (create-view-component arguments)]
        (swap! component-cache assoc arguments component)
        component))))

(defn- create-inferno-element
  [vdom-element {on-dom-event        :on-dom-event
                 input-definitions   :input-definitions
                 output-definitions  :output-definitions
                 ancestor-views-data :ancestor-views-data   ;; Optional. Needed if function locators are to be sent from a view to another.
                 view-instance       :view-instance         ;; Optional. Needed to activate view-specific input systems. If not present, only standard HTML attributes and such will be processed.
                 :as                 system-options}]
  (ensure-global-inferno!)
  (cond
    ;; React.createElement cares about children ordering, so it is important to keep nil children.
    ;; Otherwise, React won't be able to recognize unaffected children (so it will re-mount all of them).
    ;; TODO: Is this also true to Inferno?
    (nil? vdom-element)
    nil

    (string? vdom-element)
    vdom-element

    (number? vdom-element)
    (str vdom-element)

    ;; A view (tree structure of html elements with a lifecycle) is to be rendered.
    (vdom/view? vdom-element)
    (let [view vdom-element
          definition (vdom/get-view-definition view)
          input (second vdom-element)]
      (when-not definition
        (error (str "Unable to find view definition. " view)))
      (js/Inferno.createElement (get-component {:definition          definition
                                                :input-definitions   input-definitions
                                                :output-definitions  output-definitions
                                                :ancestor-views-data ancestor-views-data})
                                (if-let [key (:key input)]
                                  #js{:input input
                                      :key   key}
                                  #js{:input input})))

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [attributes (second vdom-element)
          children (nth vdom-element 2)
          inferno-element-args (concat [(name (first vdom-element))
                                        (clj->js (map-to-inferno-attributes attributes {:on-dom-event on-dom-event}))]
                                       (clj->js (if (and (= (count children) 1)
                                                         (or (string? (first children))
                                                             (number? (first children))))
                                                  [(first children)]
                                                  (map (fn [child]
                                                         (create-inferno-element child system-options))
                                                       children))))]
      (apply js/Inferno.createElement inferno-element-args))

    ;; A sequence of elements
    (vdom/element-sequence? vdom-element)
    (map (fn [node]
           (create-inferno-element node system-options))
         vdom-element)

    :else
    (throw (js/Error (str "Unknown vdom element: " vdom-element)))))

(defn render!
  [{view                :view
    target-element      :target-element
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ;; Optional. May be used to define conceptual parents of the rendered view tree (even though it is not a parent HTML-wise).
    ancestor-views-data :ancestor-views-data}]
  (ensure-global-inferno!)
  (if (nil? view)
    (js/Inferno.render nil target-element)
    (let [inferno-element (create-inferno-element view {:input-definitions   input-definitions
                                                        :output-definitions  output-definitions
                                                        :on-dom-event        (fn [_])
                                                        :ancestor-views-data ancestor-views-data})]
      (js/Inferno.render inferno-element target-element))))