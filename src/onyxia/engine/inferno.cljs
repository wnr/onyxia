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
                                                                     ;; We actually do not need to store any data in the component state.
                                                                     ;; But we need to call it in order to get a render.
                                                                     (.setState component nil)))
                                                                 nil)})))

(def lifecycle-component (js/Inferno.createClass (clj->js {:displayName          "LifecycleComponent"
                                                           :render               (fn []
                                                                                   (this-as component
                                                                                     (let [jsprops (.-onyxiaProps (.-props component))
                                                                                           vdom-element (:vdom-element jsprops)
                                                                                           system-options (:system-options jsprops)]
                                                                                       (create-inferno-element (update vdom-element 1 (fn [attributes]
                                                                                                                                        (-> attributes
                                                                                                                                            (dissoc :on-will-unmount)
                                                                                                                                            (assoc :ref (fn [element] (aset component "_lifecycleElement" element))))))
                                                                                                               system-options))))
                                                           :componentWillUnmount (fn []
                                                                                   (this-as component
                                                                                     (let [jsprops (.-onyxiaProps (.-props component))
                                                                                           vdom-element (:vdom-element jsprops)
                                                                                           system-options (:system-options jsprops)
                                                                                           on-will-unmount (:on-will-unmount (second vdom-element))]
                                                                                       (and on-will-unmount ((first on-will-unmount)
                                                                                                              {:element (aget component "_lifecycleElement")}))))
                                                                                   nil)})))

(defn- get-component
  [arguments]
  (let [cached-component (get @component-cache arguments)]
    (if cached-component
      cached-component
      (let [component (create-view-component arguments)]
        (swap! component-cache assoc arguments component)
        component))))

(defn- modify-attributes
  [attributes {view-instance :view-instance :as args}]
  (map-to-inferno-attributes (if view-instance
                               (vi/modify-attributes attributes args)
                               attributes)
                             args))

(defn inferno-element? [vdom-element]
  (not (nil? (.-type vdom-element))))

(defn- create-inferno-element
  [vdom-element {on-dom-event        :on-dom-event
                 input-definitions   :input-definitions
                 output-definitions  :output-definitions
                 ancestor-views-data :ancestor-views-data   ;; Optional. Needed if function locators are to be sent from a view to another.
                 view-instance       :view-instance         ;; Optional. Needed to activate view-specific input systems. If not present, only standard HTML attributes and such will be processed.
                 :as                 system-options}]
  (cond
    (nil? vdom-element)
    nil

    (string? vdom-element)
    vdom-element

    (number? vdom-element)
    (str vdom-element)

    ;; Already processed Inferno element
    (inferno-element? vdom-element)
    vdom-element

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [original-vdom-element vdom-element
          vdom-element (vdom/formalize-element vdom-element)
          attributes (-> (second vdom-element)
                         (modify-attributes {:view-instance view-instance
                                             :on-dom-event  on-dom-event}))]
      (if (contains? attributes :on-will-unmount)
        (js/Inferno.createElement lifecycle-component #js {:onyxiaProps {:vdom-element   (assoc original-vdom-element 1 attributes)
                                                                         :system-options system-options}
                                                           :key         (:key attributes)})
        (let [children (nth vdom-element 2)
              children (map (fn [child]
                              (create-inferno-element child system-options))
                            children)
              inferno-element-args (concat [(name (first vdom-element))
                                            (clj->js attributes)]
                                           (clj->js (map (fn [child]
                                                           (create-inferno-element child system-options))
                                                         children)))]
          (apply js/Inferno.createElement inferno-element-args))))

    ;; A view (tree structure of html elements with a lifecycle) is to be rendered.
    (vdom/view? vdom-element)
    (let [view vdom-element
          definition (vdom/get-view-definition view)
          ;input (second vdom-element)
          input (-> (vdom/get-view-input view)
                    (update :children (fn [children]
                                        (map (fn [child]
                                               (create-inferno-element child system-options))
                                             children))))]
      (when-not definition
        (error (str "Unable to find view definition. " view)))
      (js/Inferno.createElement (get-component {:definition          definition
                                                :input-definitions   input-definitions
                                                :output-definitions  output-definitions
                                                :ancestor-views-data ancestor-views-data})
                                (if-let [key (:key input)]
                                  ;; TODO: Do we need to send input like this?
                                  #js{:input input
                                      :key   key}
                                  #js{:input input})))

    ;; A sequence of elements
    (vdom/element-sequence? vdom-element)
    (reduce (fn [a node]
              (if-let [result (create-inferno-element node system-options)]
                (if (sequential? result)
                  (into a result)
                  (conj a result))
                a))
            []
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
    (let [inferno-element (create-inferno-element view
                                                  {:input-definitions   input-definitions
                                                   :output-definitions  output-definitions
                                                   :on-dom-event        (fn [_])
                                                   :ancestor-views-data ancestor-views-data})]
      (js/Inferno.render inferno-element target-element))))