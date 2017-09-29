(ns onyxia.view-instance
  (:require [ysera.error :refer [error]]
            [onyxia.dom-operator :refer [add-pending-operation!]]
            [onyxia.view-instance-utils :refer [formalize-input-definitions
                                                formalize-output-definitions]]))

(defn create-view-instance
  [{definition          :definition
    input-definitions   :input-definitions
    output-definitions  :output-definitions
    ancestor-views-data :ancestor-views-data
    render!             :render!}]
  (atom {:definition           definition
         :input-definitions    (formalize-input-definitions input-definitions)
         :output-definitions   (formalize-output-definitions output-definitions)
         :ancestor-views-data  ancestor-views-data
         :render!              render!
         :input-instances-data nil
         :parent-element       nil
         :parent-input         nil
         :mounted              false
         :has-been-mounted     false
         :id                   (str (js/Math.random))}))

(defn get-id
  [view-instance]
  (:id (deref view-instance)))

(defn get-definition
  [view-instance]
  (:definition (deref view-instance)))

(defn get-input-definitions
  [view-instance]
  (:input-definitions (deref view-instance)))

(defn get-output-definitions
  [view-instance]
  (:output-definitions (deref view-instance)))

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
       (vals)
       (map :instance)))

(defn init-input-systems!
  [view-instance {on-state-changed :on-state-changed
                  root-element     :root-element}]
  {:pre [view-instance on-state-changed]}
  (let [definition (get-definition view-instance)
        input-definitions (get-input-definitions view-instance)]
    (set-input-system-instances-data! view-instance (reduce-kv (fn [inputs-state input-name input-options]
                                                                 (let [[input-definition predefined-options] (get-input-definition input-definitions (:name input-options))]
                                                                   (assoc inputs-state
                                                                     input-name
                                                                     {:instance ((:get-instance input-definition)
                                                                                  (merge predefined-options
                                                                                         (dissoc input-options :name)
                                                                                         {:on-state-changed on-state-changed
                                                                                          :root-element     root-element}))})))
                                                               {}
                                                               (:input definition)))))

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

(defn set-parent-input!
  [view-instance parent-input]
  (swap! view-instance assoc :parent-input parent-input))

(defn get-parent-input
  [view-instance]
  (:parent-input (deref view-instance)))

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

(defn get-view-input
  [view-instance]
  (reduce-kv (fn [view-input input-name {instance :instance}]
               (assoc view-input
                 input-name
                 ((:get-value instance))))
             (merge (get-parent-input view-instance)
                    {:view-state (deref (get-view-state-atom view-instance))})
             (get-input-system-instances-data view-instance)))

(defn should-render?
  [view-instance]
  (and (or (mounted? view-instance)
           (not (has-been-mounted? view-instance)))
       (let [definition (get-definition view-instance)]
         (let [should-render?-fn (:should-render? definition)]
           (if should-render?-fn
             (should-render?-fn (get-view-input view-instance))
             true)))))

(defn handle-output-systems!
  [view-instance]
  (when (should-render? view-instance)
    (let [definition (get-definition view-instance)
          input-definitions (get-input-definitions view-instance)
          output-definitions (get-output-definitions view-instance)]
      (reduce (fn [_ output]
                (let [[output-definition predefined-options] (get-output-definition output-definitions (:name output))]
                  ((:handle! output-definition)
                    (merge predefined-options
                           {:view-output         output
                            :view-state          (get-view-input view-instance)
                            :render!             (get-render-fn view-instance)
                            :input-definitions   input-definitions
                            :output-definitions  output-definitions
                            :ancestor-views-data (assoc (get-ancestor-views-data view-instance) definition {:view-state-atom (get-view-state-atom view-instance)})
                            :view-instance-id    (get-id view-instance)}))))
              nil
              (:output definition)))))

(defn will-mount!
  [view-instance {parent-input     :parent-input
                  on-state-changed :on-state-changed
                  root-element     :root-element}]
  (let [handle-state-change (fn []
                              (handle-output-systems! view-instance)
                              (on-state-changed))]
    (init-view-state! view-instance)
    (set-parent-input! view-instance parent-input)
    (init-input-systems! view-instance {:on-state-changed handle-state-change
                                        :root-element     root-element})
    (add-watch (get-view-state-atom view-instance)
               :on-state-changed-notifier
               (fn [_ _ _ _] (handle-state-change)))
    (add-watch view-instance
               :on-state-changed-notifier
               (fn [_ _ old-value new-value]
                 (when (not= (:parent-input old-value) (:parent-input new-value))
                   (handle-state-change))))))

(defn did-render!
  [view-instance]
  (let [definition (get-definition view-instance)]
    (when-let [did-render-fn (:did-render definition)]
      ;; TODO: Not nice to merge like this. What if some input to this view is called "element"?
      (did-render-fn (merge {:element                (get-parent-element view-instance)
                             :add-pending-operation! add-pending-operation!}
                            (get-view-input view-instance))))))

(defn did-mount!
  [view-instance {parent-element :parent-element}]
  ;; TODO: Should probably be signaled to view as well.
  (set-parent-element! view-instance parent-element)
  (set-has-been-mounted view-instance true)
  (set-mounted view-instance true)
  (doseq [input-instance (get-input-system-instances view-instance)]
    (when (:did-mount input-instance)
      (add-pending-operation! ((:did-mount input-instance)
                                {;; TODO: Should this be the root element of the view instead?
                                 :element parent-element}))))
  (did-render! view-instance))

(defn will-unmount!
  [view-instance]
  ;; TODO: Should probably be signaled to view as well.
  (set-mounted view-instance true)
  (doseq [input-instances (get-input-system-instances view-instance)]
    (when (:will-unmount input-instances)
      ;; Not adding to a pending operation here, because we do not have control of React/frameworks.
      ;; We want to call the unmount signal before the view is unmounted.
      ;; TODO: Can we take control over this?
      ((:will-unmount input-instances)
        {;; TODO: Should this be the root element of the view instead?
         :element (get-parent-element view-instance)}))))

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

(defn modify-attributes
  [attributes {view-instance :view-instance}]
  (reduce (fn [attributes input-instance]
            (if-let [element-attribute-modifier (:element-attribute-modifier input-instance)]
              (if-let [modified-attributes (element-attribute-modifier {:attributes attributes})]
                modified-attributes
                attributes)
              attributes))
          attributes
          (get-input-system-instances view-instance)))