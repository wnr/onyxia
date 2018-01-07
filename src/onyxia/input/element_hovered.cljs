(ns onyxia.input.element-hovered
  (:require [onyxia.attributes :refer [add-event-handler]]
            [ysera.error :refer [error]]
            ["ua-parser-js" :as ua-parser]))

(def ua (ua-parser))

(defn disable-system?
  []
  (let [device-type (aget ua "device" "type")]
    (or (= device-type "mobile")
        (= device-type "tablet"))))

(def definition
  {:name         "element-hovered"
   :get-instance (fn [{on-state-changed                :on-state-changed
                       still-to-initiate-hover         :still-to-initiate-hover
                       still-to-remain-hovered         :still-to-remain-hovered
                       mouse-position-input-definition :mouse-position-input-definition
                       should-update?                  :should-update?}]
                   (when (and still-to-initiate-hover (not mouse-position-input-definition))
                     (error ":mouse-position-input-definition is required when :still is true."))
                   (when (and (not still-to-initiate-hover) still-to-remain-hovered)
                     (error ":still-to-remain-hovered cannot be true without :still-to-initiate-hover being true. That doesn't make sense."))
                   (if (disable-system?)
                     {:ready? (fn [] true)
                      :get-value (fn [] nil)}
                     (let [should-update? (or should-update? (fn [] true))
                           state-atom (atom {:hover-value      nil
                                             :still            false
                                             :still-timeout-id nil})
                           on-still-timeout (fn []
                                              (when (should-update?)
                                                (swap! state-atom assoc :still true :still-timeout-id nil)))
                           get-value (fn [state]
                                       (when-let [value (and (or (not still-to-initiate-hover) (:still state))
                                                             (:hover-value state))]
                                         value))
                           mouse-position-instance (when still-to-initiate-hover
                                                     ((:get-instance mouse-position-input-definition)
                                                       {:on-state-changed (fn []
                                                                            (let [state (deref state-atom)]
                                                                              (when-let [still-timeout-id (:still-timeout-id state)]
                                                                                (js/clearTimeout still-timeout-id))
                                                                              (when (and (:hover-value state)
                                                                                         (or still-to-remain-hovered
                                                                                             (not (:still state))))
                                                                                (when (should-update?)
                                                                                  (swap! state-atom assoc :still false :still-timeout-id (js/setTimeout on-still-timeout 100))))))}))]
                       (add-watch state-atom :state-change-notifier (fn [_ _ old-state new-state]
                                                                      (when (not= (get-value old-state) (get-value new-state))
                                                                        (on-state-changed))))
                       {:ready?                     (fn [] true)
                        :get-value                  (fn [] (get-value (deref state-atom)))
                        :element-attribute-modifier (fn [{attributes :attributes}]
                                                      (when (contains? attributes :element-hovered-value)
                                                        (-> attributes
                                                            (dissoc :element-hovered-value)
                                                            (add-event-handler :on-mouse-enter (fn []
                                                                                                 (when (should-update?)
                                                                                                   (swap! state-atom assoc :hover-value (:element-hovered-value attributes) :still false))))
                                                            (add-event-handler :on-mouse-leave (fn []
                                                                                                 (when (should-update?)
                                                                                                   (swap! state-atom assoc :hover-value nil :still false)))))))
                        :will-unmount               (fn [args]
                                                      (when mouse-position-instance
                                                        ((:will-unmount mouse-position-instance) args)))})))})
