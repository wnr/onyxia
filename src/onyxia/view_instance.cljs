(ns onyxia.view-instance
  (:require [ysera.error :refer [error]]
            [onyxia.dom-operator :refer [add-pending-operation!]]
            [onyxia.view-instance-utils :refer [formalize-input-definitions
                                                formalize-output-definitions]]
            [onyxia.vdom :as vdom]))

(def system-state-atom (atom (:counter 0)))

(defn generate-new-view-id!
  []
  (:counter (swap! system-state-atom update :counter inc)))

(defn create-view-instance
  [{definition                   :definition
    available-input-definitions  :input-definitions
    available-output-definitions :output-definitions
    ancestor-views-data          :ancestor-views-data
    render!                      :render!}]
  (atom {:definition                   definition
         :available-input-definitions  (formalize-input-definitions available-input-definitions)
         :input-instances-data         nil
         :available-output-definitions (formalize-output-definitions available-output-definitions)
         :output-instances-data        nil
         :ancestor-views-data          ancestor-views-data
         :render!                      render!
         :parent-element               nil
         :parent-input                 nil
         :mounted                      false
         :has-been-mounted             false
         :last-rendered-view-input     nil
         :current-view-input           nil
         :id                           (generate-new-view-id!)}))

(defn get-id
  [view-instance]
  (:id (deref view-instance)))

(defn get-definition
  [view-instance]
  (:definition (deref view-instance)))

(defn get-available-input-definitions
  [view-instance]
  (:available-input-definitions (deref view-instance)))

(defn get-available-output-definitions
  [view-instance]
  (:available-output-definitions (deref view-instance)))

(defn get-ancestor-views-data
  [view-instance]
  (:ancestor-views-data (deref view-instance)))

(defn get-input-definition
  [input-definitions name]
  (let [input-definition (get input-definitions name)]
    (when (not input-definition)
      (error "No input-definition found for input " name))
    input-definition))

(defn get-output-definition
  [output-definitions name]
  (let [output-definition (get output-definitions name)]
    (when (not output-definition)
      (error "No output-definition found for output " name))
    output-definition))

(defn get-ancestor-view-instance-atom
  [view-instance {ancestor-definition :ancestor-definition}]
  (let [view-data (get (get-ancestor-views-data view-instance) ancestor-definition)]
    (when (not view-data)
      (error "Unable to find ancestor view data for ancestor definition" (:name ancestor-definition)))
    (:view-state-atom view-data)))

(defn set-input-system-instances-data!
  [view-instance value]
  {:pre [view-instance]}
  (swap! view-instance assoc :input-instances-data value))

(defn get-input-system-instances-data
  [view-instance]
  {:pre [view-instance]}
  (:input-instances-data (deref view-instance)))

(defn get-input-system-instances
  [view-instance]
  {:pre [view-instance]}
  (->> (get-input-system-instances-data view-instance)
       (map :instance)))

(defn init-input-systems!
  "Inits input system instances for a view instance."
  [view-instance {on-state-changed :on-state-changed
                  root-element     :root-element}]
  {:pre [view-instance on-state-changed]}
  (let [definition (get-definition view-instance)
        available-input-definitions (get-available-input-definitions view-instance)]
    (swap! view-instance (fn [view-instance-data]
                           ; create a map that contains input instances and the configuration of them.
                           ; Important to not use map or any other lazy operation here, since lazyness and mutation does not go well together.
                           (let [input-instances-data (reduce (fn [a input]
                                                                (conj a
                                                                      (let [[input-definition render-tree-input-system-options] (get-input-definition available-input-definitions (:name input))]
                                                                        {:instance ((:get-instance input-definition)
                                                                                     (merge render-tree-input-system-options
                                                                                            (dissoc input :name)
                                                                                            {:on-state-changed on-state-changed
                                                                                             :root-element     root-element}))})))
                                                              '()
                                                              (:input definition))]
                             (assoc view-instance-data :input-instances-data input-instances-data
                                                       :attribute-modifiers (let [modifiers (reduce (fn [a input-instances-data]
                                                                                                      (if-let [modifier (get-in input-instances-data [:instance :element-attribute-modifier])]
                                                                                                        (conj a)
                                                                                                        a))
                                                                                                    '()
                                                                                                    input-instances-data)]
                                                                              (if (empty? modifiers)
                                                                                nil
                                                                                modifiers))))))))

(defn set-output-system-instances-data!
  [view-instance value]
  {:pre [view-instance]}
  (swap! view-instance assoc :output-instances-data value))

(defn get-output-system-instances-data
  [view-instance]
  {:pre [view-instance]}
  (:output-instances-data (deref view-instance)))

(defn get-output-system-instances
  [view-instance]
  {:pre [view-instance]}
  (->> (get-output-system-instances-data view-instance)
       (map :instance)))

(defn init-output-systems!
  [view-instance {on-state-changed :on-state-changed
                  root-element     :root-element}]
  (let [definition (get-definition view-instance)
        available-output-defintions (get-available-output-definitions view-instance)]
    (set-output-system-instances-data! view-instance
                                       ; create a seq that contains output definitions and the configuration of them.
                                       ; Important to not use map or any other lazy operation here, since lazyness and mutation does not go well together.
                                       (reduce (fn [a output]
                                                 (conj a
                                                       (let [[output-definition render-tree-input-system-options] (get-output-definition available-output-defintions (:name output))]
                                                         {:instance                         output-definition
                                                          :options                          output
                                                          :render-tree-input-system-options render-tree-input-system-options})))
                                               '()
                                               (:output definition)))))

(defn all-input-system-instances-ready?
  [view-instance]
  {:pre [view-instance]}
  (let [input-instances (get-input-system-instances view-instance)]
    (if (empty? input-instances)
      true
      (->> input-instances
           (some (fn [input-instance]
                   (not ((:ready? input-instance)))))
           (not)))))

(defn get-view-state-atom
  [view-instance]
  (:view-state-atom (deref view-instance)))

(defn set-view-state-atom!
  [view-instance view-state-atom]
  (swap! view-instance assoc :view-state-atom view-state-atom))

(defn init-view-state!
  [view-instance]
  (let [definition (get-definition view-instance)]
    (set-view-state-atom! view-instance (atom (when (:get-initial-state definition)
                                                ((:get-initial-state definition)))))))
(defn set-parent-element!
  [view-instance parent-element]
  (swap! view-instance assoc :parent-element parent-element))

(defn get-parent-element
  [view-instance]
  (:parent-element (deref view-instance)))

(defn set-mounted
  [view-instance value]
  (swap! view-instance assoc :mounted value))

(defn mounted?
  [view-instance]
  (:mounted (deref view-instance)))

(defn set-has-been-mounted
  [view-instance value]
  (swap! view-instance assoc :has-been-mounted value))

(defn has-been-mounted?
  [view-instance]
  (:has-been-mounted (deref view-instance)))

(defn get-render-fn
  [view-instance]
  (:render! (deref view-instance)))

(defn set-last-rendered-input
  [view-instance input]
  (swap! view-instance assoc :last-rendered-view-input input))

(defn get-last-rendered-view-input
  [view-instance]
  (:last-rendered-view-input (deref view-instance)))

(defn get-parent-input
  [view-instance]
  (:parent-input (deref view-instance)))

(defn compute-view-input
  [view-instance]
  (reduce (fn [view-input {instance :instance}]
            (merge view-input
                   ((:get-input instance))))
          (merge (get-parent-input view-instance)
                 {:view-state (deref (get-view-state-atom view-instance))})
          (get-input-system-instances-data view-instance)))

(defn get-view-input
  [view-instance]
  (:current-view-input (deref view-instance)))

(defn get-view-input-children
  [view-instance]
  (:children (get-view-input view-instance)))

(defn set-view-input!
  [view-instance view-input]
  (swap! view-instance assoc :current-view-input view-input))

(defn set-parent-input!
  [view-instance parent-input]
  (when (not= parent-input
              (get-parent-input view-instance))
    (swap! view-instance assoc :parent-input parent-input)
    (set-view-input! view-instance (compute-view-input view-instance))))

(defn should-render?
  [view-instance]
  (and (or (mounted? view-instance)
           (not (has-been-mounted? view-instance)))
       (let [definition (get-definition view-instance)]
         (let [should-render?-fn (:should-render? definition)]
           (if should-render?-fn
             (should-render?-fn (get-view-input view-instance))
             true)))
       (not= (get-last-rendered-view-input view-instance)
             (get-view-input view-instance))))

(defn handle-output-systems!
  [view-instance]
  (when (should-render? view-instance)
    (let [definition (get-definition view-instance)
          available-input-definitions (get-available-input-definitions view-instance)
          available-output-definitions (get-available-output-definitions view-instance)]
      (doseq [{instance                         :instance
               options                          :options
               render-tree-input-system-options :render-tree-input-system-options} (get-output-system-instances-data view-instance)]
        ((:handle! instance)
          (merge render-tree-input-system-options
                 {:view-output         options
                  :view-state          (get-view-input view-instance)
                  :view-state-atom     (get-view-state-atom view-instance)
                  :render!             (get-render-fn view-instance)
                  :input-definitions   available-input-definitions
                  :output-definitions  available-output-definitions
                  :ancestor-views-data (assoc (get-ancestor-views-data view-instance) definition {:view-state-atom (get-view-state-atom view-instance)})
                  :view-instance-id    (get-id view-instance)}))))))

(defn will-mount!
  [view-instance {parent-input     :parent-input
                  on-state-changed :on-state-changed
                  root-element     :root-element}]
  (let [handle-possible-state-change (fn []
                                       (let [view-input (compute-view-input view-instance)]
                                         (when (not= (get-last-rendered-view-input view-instance)
                                                     view-input)
                                           (set-view-input! view-instance view-input)
                                           (handle-output-systems! view-instance)
                                           (on-state-changed))))]
    (init-view-state! view-instance)
    (set-parent-input! view-instance parent-input)
    (init-input-systems! view-instance {:on-state-changed handle-possible-state-change
                                        :root-element     root-element})
    (init-output-systems! view-instance {:on-state-changed handle-possible-state-change
                                         :root-element     root-element})
    (add-watch (get-view-state-atom view-instance)
               :on-state-changed-notifier
               (fn [_ _ old-value new-value]
                 (handle-possible-state-change)))
    (add-watch view-instance
               :on-state-changed-notifier
               (fn [_ _ old-value new-value]
                 (when (not= (:parent-input old-value) (:parent-input new-value))
                   (handle-possible-state-change))))))

(defn- get-attribute-modifiers
  [view-instance]
  (:attribute-modifiers (deref view-instance)))

(defn- modify-attributes
  [attributes {view-instance :view-instance}]
  (let [modifiers (get-attribute-modifiers view-instance)]
    (if (nil? modifiers)
      attributes
      (reduce (fn [attributes modifier]
                (if-let [modified-attributes (modifier {:attributes attributes})]
                  modified-attributes
                  attributes))
              attributes
              modifiers))))

(defn- prepare-element-tree
  [vdom-element {view-instance :view-instance :as system-options}]
  (cond
    (and view-instance
         (= vdom-element (get-view-input-children view-instance)))
    vdom-element

    ;; A view (tree structure of html elements with a lifecycle) is to be rendered.
    (vdom/view? vdom-element)
    (let [view vdom-element
          input (-> (vdom/get-view-input view)
                    (update :children (fn [children]
                                        (map (fn [child]
                                               (prepare-element-tree child system-options))
                                             children))))]
      [(first vdom-element) input])

    ;; A "normal" HTML DOM element.
    (vdom/element? vdom-element)
    (let [vdom-element (vdom/formalize-element vdom-element)
          attributes (second vdom-element)
          children (nth vdom-element 2)
          new-attributes (if view-instance
                           (modify-attributes attributes {:view-instance view-instance})
                           attributes)
          new-children (map (fn [child]
                              (prepare-element-tree child system-options))
                            children)]
      [(first vdom-element) new-attributes new-children])

    ;; A sequence of elements (that will need to be handled as such with keys etc).
    (vdom/element-sequence? vdom-element)
    (map-indexed (fn [index node]
                   (let [node (if (vdom/element? node)
                                (vdom/ensure-attributes-map node)
                                node)]
                     (prepare-element-tree node system-options)))
                 (vdom/clean-element-sequence vdom-element))

    :else
    vdom-element))

(defn trigger-view-instance-event!
  [view-instance event-name data]
  (let [data (merge data
                    ;; TODO: Not nice to merge like this.
                    (get-view-input view-instance))
        possible-observers (concat [(get-definition view-instance)]
                                   (get-input-system-instances view-instance)
                                   (get-output-system-instances view-instance))]
    (doseq [possible-observer possible-observers]
      (when-let [event-fn (event-name possible-observer)]
        (event-fn data)))))

(defn render! [view-instance]
  (let [input (merge (get-view-input view-instance)
                     {:view-state-atom (get-view-state-atom view-instance)})]
    (set-last-rendered-input view-instance input)
    (let [definition (get-definition view-instance)
          vdom-element ((:render definition) input)]
      vdom-element
      ;;(prepare-element-tree vdom-element {:view-instance view-instance})
      )))

(defn did-render!
  [view-instance]
  (trigger-view-instance-event! view-instance
                                :did-render
                                {:element                (get-parent-element view-instance)
                                 :add-pending-operation! add-pending-operation!
                                 :view-state-atom        (get-view-state-atom view-instance)}))

(defn did-mount!
  [view-instance {parent-element :parent-element}]
  ;; TODO: Should probably be signaled to view as well.
  (set-parent-element! view-instance parent-element)
  (set-has-been-mounted view-instance true)
  (set-mounted view-instance true)
  (trigger-view-instance-event! view-instance
                                :did-mount
                                {:element (get-parent-element view-instance)})
  (did-render! view-instance))

(defn will-unmount!
  [view-instance]
  (set-mounted view-instance false)
  (trigger-view-instance-event! view-instance
                                :will-unmount
                                {:view-instance-id (get-id view-instance)
                                 :element          (get-parent-element view-instance)}))

(defn handle-dom-event
  [view-instance {handlers :handlers
                  event    :event}]
  (let [definition (get-definition view-instance)]
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
              handle-fn-data (merge (when (not= (last handler) handle-fn-key)
                                      (last handler))
                                    {:event event})]
          (if (not handle-fn)
            (throw (error (str "Cannot find " handle-fn-key " function in definition " (:name event-handling-definition))))
            (let [handling-view-state-atom (if (not= event-handling-definition definition)
                                             (get-ancestor-view-instance-atom view-instance {:ancestor-definition event-handling-definition})
                                             (get-view-state-atom view-instance))]
              ;; TODO: Could avoid multiple swaps by merging all handler functions into a single composite transform.
              (swap! handling-view-state-atom (fn [state]
                                                (if-let [result (handle-fn state handle-fn-data)]
                                                  result
                                                  state))))))
        ;; A raw function has been passed as handler, simply invoke it.
        (handler event)))))