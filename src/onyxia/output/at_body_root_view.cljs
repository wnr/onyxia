(ns onyxia.output.at-body-root-view)

(def system-atom (atom {:container-element nil
                        :current-view      nil
                        :wanted-view       nil}))

(defn get-container-element!
  []
  (:container-element @system-atom))

(defn create-container-element!
  []
  (let [container-element (.createElement js/document "div")]
    (.setAttribute container-element "data-entity" "at-body-root-view-container")
    (.appendChild (.-body js/document) container-element)
    (swap! system-atom assoc :container-element container-element)))

(defn set-wanted-view-context
  [state view-context]
  (assoc state :wanted-view-context view-context))

(defn get-wanted-view-context
  [state]
  (:wanted-view-context state))

(defn clear-wanted-view-context
  [state]
  (assoc state :wanted-view-context nil))

(defn get-current-view-context
  [state]
  (:current-view-context state))

(defn set-current-view-context
  [state view-context]
  (assoc state :current-view-context view-context))

(defn handle-current-view! []
  (let [state @system-atom
        wanted-view-context (get-wanted-view-context state)
        current-view-context (get-current-view-context state)]
    (when (not= wanted-view-context current-view-context)
      (if wanted-view-context
        ((:render! wanted-view-context)
          {:view                (:view wanted-view-context)
           :target-element      (:container-element @system-atom)
           :input-definitions   (:input-definitions wanted-view-context)
           :output-definitions  (:output-definitions wanted-view-context)
           :ancestor-views-data (:ancestor-views-data wanted-view-context)})
        (when current-view-context
          ((:render! current-view-context)
            {:target-element (:container-element @system-atom)
             :view           nil})))
      (swap! system-atom set-current-view-context wanted-view-context))))

(add-watch system-atom
           :view-updater
           (fn [key atom old-state new-state]
             (handle-current-view!)))

(if (nil? (get-container-element!))
  (create-container-element!)
  (swap! system-atom set-current-view-context nil))

(def definition
  {:name    "at-body-root-view"
   :handle! (fn [{view-output         :view-output
                  view-state          :view-state
                  render!             :render!
                  input-definitions   :input-definitions
                  output-definitions  :output-definitions
                  ancestor-views-data :ancestor-views-data}]
              (let [view ((:get-view view-output) view-state)]
                (if view
                  (swap! system-atom set-wanted-view-context {:view                view
                                                              :render!             render!
                                                              :input-definitions   input-definitions
                                                              :output-definitions  output-definitions
                                                              :ancestor-views-data ancestor-views-data})
                  (swap! system-atom clear-wanted-view-context))))})