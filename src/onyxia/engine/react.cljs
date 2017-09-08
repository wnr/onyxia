(ns onyxia.engine.react
  (:require [cljsjs.react.dom]
            [onyxia.engine.react-utils :refer [map-to-react-attributes
                                               add-key-attribute]]
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
                                                                                                  :on-state-changed (fn [] (.onStateChanged component))
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
                                                                     (create-react-element ((:render definition)
                                                                                             (vi/get-view-input view-instance))
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

(defn- create-react-element
  [vdom-element {on-dom-event        :on-dom-event
                 input-definitions   :input-definitions
                 output-definitions  :output-definitions
                 ancestor-views-data :ancestor-views-data   ;; Optional. Needed if function locators are to be sent from a view to another.
                 view-instance       :view-instance         ;; Optional. Needed to activate view-specific input systems. If not present, only standard HTML attributes and such will be processed.
                 root-element        :root-element
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
    (vdom/view? vdom-element)
    (let [view vdom-element
          definition (vdom/get-view-definition view)
          input (vdom/get-view-input view)]
      (when-not definition
        (error (str "Unable to find view definition. " view)))
      (js/React.createElement (get-component {:definition          definition
                                              :input-definitions   input-definitions
                                              :output-definitions  output-definitions
                                              :ancestor-views-data ancestor-views-data
                                              :root-element        root-element})
                              #js{:input input}))

    ;; A "normal" HTML DOM element.
    (keyword? (first vdom-element))
    (let [vdom-element (vdom/formalize-element vdom-element)
          attributes (second vdom-element)
          children (nth vdom-element 2)
          react-element-args (concat [(name (first vdom-element))
                                      (clj->js (-> (if view-instance
                                                     (vi/modify-attributes attributes {:view-instance view-instance})
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
                                                    :ancestor-views-data ancestor-views-data
                                                    :root-element        target-element})]
      (js/ReactDOM.render react-element target-element))))
