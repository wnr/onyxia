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
              (let [current-view-context (get-current-view-context @system-atom)]
                (when (or (nil? current-view-context)
                          (= view-output (:view-output current-view-context)))
                  ;; If there is no current at-body-root-view, it is free for any view to request one.
                  ;; If there is an active at-body-root-view, only the view that requested it may remove it.
                  ;; It might be nice to be able to define custom priority strategies in the future.
                  (let [at-body-root-view ((:get-view view-output) view-state)]
                    (if at-body-root-view
                      (swap! system-atom set-wanted-view-context {:view                at-body-root-view
                                                                  :render!             render!
                                                                  :input-definitions   input-definitions
                                                                  :output-definitions  output-definitions
                                                                  :ancestor-views-data ancestor-views-data
                                                                  ;; We add this so that we can make sure that only the view that
                                                                  ;; once added the at-body-root-view may clear it.
                                                                  :view-output         view-output})
                      (swap! system-atom clear-wanted-view-context))))))})