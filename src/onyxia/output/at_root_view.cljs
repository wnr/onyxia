(ns onyxia.output.at-root-view
  (:require [ysera.error :refer [error]]))

(def system-atom (atom {:current-view nil
                        :wanted-view  nil}))

;(defn create-container-element!
;  []
;  (let [container-element (.createElement js/document "div")]
;    (.setAttribute container-element "data-entity" "at-root-view-container")
;    (.appendChild (.-body js/document) container-element)
;    (swap! system-atom assoc :container-element container-element)))

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
           :target-element      (:target-element wanted-view-context)
           :input-definitions   (:input-definitions wanted-view-context)
           :output-definitions  (:output-definitions wanted-view-context)
           :ancestor-views-data (:ancestor-views-data wanted-view-context)})
        (when current-view-context
          ((:render! current-view-context)
            {:target-element (:target-element current-view-context)
             :view           nil})))
      (swap! system-atom set-current-view-context wanted-view-context))))

(add-watch system-atom
           :view-updater
           (fn [key atom old-state new-state]
             (handle-current-view!)))

(def definition
  {:name         "at-root-view"
   :handle!      (fn [{view-output         :view-output
                       view-state          :view-state
                       render!             :render!
                       input-definitions   :input-definitions
                       output-definitions  :output-definitions
                       ancestor-views-data :ancestor-views-data
                       view-instance-id    :view-instance-id
                       target-element      :target-element}]
                   (when (not target-element)
                     (error "at-root-view: Required parameter ':target-element' is not set."))
                   (let [current-view-context (get-current-view-context @system-atom)]
                     (when (or (nil? current-view-context)
                               (= view-instance-id (:view-instance-id current-view-context)))
                       ;; If there is no current at-root-view, it is free for any view to request one.
                       ;; If there is an active at-root-view, only the view that requested it may remove it.
                       ;; It might be nice to be able to define custom priority strategies in the future.
                       (let [at-root-view ((:get-view view-output) view-state)]
                         (if at-root-view
                           (swap! system-atom set-wanted-view-context {:view                at-root-view
                                                                       :render!             render!
                                                                       :input-definitions   input-definitions
                                                                       :output-definitions  output-definitions
                                                                       :ancestor-views-data ancestor-views-data
                                                                       :target-element      target-element
                                                                       ;; We add this so that we can make sure that only the view instance that
                                                                       ;; once added the at-root-view may clear it.
                                                                       :view-instance-id    view-instance-id})
                           (swap! system-atom clear-wanted-view-context))))))
   :will-unmount (fn [{view-instance-id :view-instance-id}]
                   (let [current-view-context (get-current-view-context @system-atom)]
                     (when (= view-instance-id (:view-instance-id current-view-context))
                       ;; When the view that currently is the active view context unmounts, we need to remove it as
                       ;; active view context.
                       ;; TODO: the handle functions of all other views should be invoked, since another view might want to show something now that there are room.
                       (swap! system-atom clear-wanted-view-context))))})