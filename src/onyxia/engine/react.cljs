(ns onyxia.engine.react
  (:require [cljsjs.react.dom]
            [onyxia.engine.react-utils :refer [map-to-react-attributes]]
            [onyxia.vdom :as vdom]
            [onyxia.view-instance :as vi]
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

(defn- component->parent-element
  [component]
  (let [internal-instance (aget component "_reactInternalInstance")
        host-parent (aget internal-instance "_hostParent")]
    (or (and host-parent (js/ReactDOM.findDOMNode (aget host-parent "_hostNode")))
        (aget internal-instance "_hostContainerInfo" "_node"))))

(defn create-view-component
  [{definition          :definition
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ancestor-views-data :ancestor-views-data
    root-element        :root-element
    :as                 args}]
  (js/React.createClass (clj->js {:displayName               (:name definition)
                                  :componentWillMount        (fn []
                                                               (this-as component
                                                                 (let [view-instance (vi/create-view-instance (merge args {:render! render!}))]
                                                                   (aset component "viewInstance" view-instance)
                                                                   (vi/will-mount! view-instance {:parent-input     (aget component "props" "input")
                                                                                                  :on-state-changed (fn []
                                                                                                                      ((aget component "onStateChanged")))
                                                                                                  :root-element     root-element})))
                                                               nil)
                                  :componentDidMount         (fn []
                                                               (this-as component
                                                                 (vi/did-mount! (aget component "viewInstance") {:parent-element (component->parent-element component)}))
                                                               nil)
                                  :render                    (fn []
                                                               (this-as component
                                                                 (let [view-instance (aget component "viewInstance")]
                                                                   (when (vi/all-input-system-instances-ready? view-instance)
                                                                     (create-react-element (vi/render! view-instance)
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
                                                                   (.setState component #js{})))
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
  (map-to-react-attributes (if view-instance
                             (vi/modify-attributes attributes args)
                             attributes)
                           args))

(defn react-element? [vdom-element]
  (not (nil? (.-$$typeof vdom-element))))

(defn- create-react-element
  [vdom-element {on-dom-event        :on-dom-event
                 input-definitions   :input-definitions
                 output-definitions  :output-definitions
                 ancestor-views-data :ancestor-views-data   ;; Optional. Needed if function locators are to be sent from a view to another.
                 view-instance       :view-instance         ;; Optional. Needed to activate view-specific input systems. If not present, only standard HTML attributes and such will be processed.
                 root-element        :root-element
                 key                 :key
                 :as                 system-options}]
  (cond
    (nil? vdom-element)
    nil

    (string? vdom-element)
    vdom-element

    (number? vdom-element)
    (str vdom-element)

    ;; Already processed React element
    (react-element? vdom-element)
    vdom-element

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [vdom-element (vdom/formalize-element vdom-element)
          attributes (second vdom-element)
          children (nth vdom-element 2)
          attributes (if key
                       (update attributes :key (fn [k]
                                                 (or k key)))
                       attributes)
          attributes (modify-attributes attributes {:view-instance view-instance
                                                    :on-dom-event  on-dom-event})
          children (map (fn [child]
                          (create-react-element child system-options))
                        children)
          system-options (dissoc system-options :key)
          react-element-args (concat [(name (first vdom-element))
                                      (clj->js attributes)]
                                     (clj->js (map (fn [child]
                                                     (create-react-element child system-options))
                                                   children)))]
      (apply js/React.createElement react-element-args))

    ;; A view (tree structure of html elements with a lifecycle) is to be rendered.
    (vdom/view? vdom-element)
    (let [view vdom-element
          definition (vdom/get-view-definition view)
          system-options (dissoc system-options :key)
          input (-> (vdom/get-view-input view)
                    (update :children (fn [children]
                                        (map (fn [child]
                                               (create-react-element child system-options))
                                             children))))]
      (js/React.createElement (get-component {:definition          definition
                                              :input-definitions   input-definitions
                                              :output-definitions  output-definitions
                                              :ancestor-views-data ancestor-views-data
                                              :root-element        root-element})
                              (if-let [key (or (:key input) key)]
                                ;; TODO: Do we need to send input like this?
                                #js{:input input
                                    :key   key}
                                #js{:input input})))

    (vdom/element-sequence? vdom-element)
    (reduce (fn [a [index node]]
              (if-let [result (create-react-element node (assoc system-options :key index))]
                (if (sequential? result)
                  (into a result)
                  (conj a result))
                a))
            []
            (map-indexed (fn [x y] [x y]) vdom-element))

    :else
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
    (let [react-element (create-react-element view
                                              {:input-definitions   input-definitions
                                               :output-definitions  output-definitions
                                               :on-dom-event        (fn [_])
                                               :ancestor-views-data ancestor-views-data})]
      (js/ReactDOM.render react-element target-element))))
