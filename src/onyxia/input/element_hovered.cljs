(ns onyxia.input.element-hovered
  (:require [onyxia.attributes :refer [add-event-handler]]
            [ysera.error :refer [error]]
            [goog.labs.userAgent.device :as device]))

(defn disable-system?
  []
  (not (device/isDesktop)))

(defn get-value-of-key [state key still-to-initiate-hover]
  (if (or (not still-to-initiate-hover) (:still state))
    (get-in state [:hover-values key])
    nil))

(defn get-input [state keys still-to-initiate-hover]
  (reduce (fn [a {input-key :input}]
            (assoc a input-key (get-value-of-key state input-key still-to-initiate-hover)))
          {}
          keys))

(defn anyting-hovered?
  [state]
  (->> (:hover-values state)
       (vals)
       (remove nil?)
       (empty?)
       (not)))

(def definition
  {:name         "element-hovered"
   :get-instance (fn [{on-state-changed                :on-state-changed
                       still-to-initiate-hover         :still-to-initiate-hover
                       still-to-remain-hovered         :still-to-remain-hovered
                       mouse-position-input-definition :mouse-position-input-definition
                       should-update?                  :should-update?
                       keys                            :keys}]
                   (when (and still-to-initiate-hover (not mouse-position-input-definition))
                     (error ":mouse-position-input-definition is required when :still is true."))
                   (when (and (not still-to-initiate-hover) still-to-remain-hovered)
                     (error ":still-to-remain-hovered cannot be true without :still-to-initiate-hover being true. That doesn't make sense."))
                   (if (disable-system?)
                     ;; TODO: fix for multiple keys
                     (comment {:ready?                     (fn [] true)
                               :get-input                  (fn []
                                                             {input-key nil})
                               :element-attribute-modifier (fn [{attributes :attributes}]
                                                             (when (contains? attributes :element-hovered-value)
                                                               (dissoc attributes :element-hovered-value)))})
                     (let [input-keys (map :input keys)
                           should-update? (or should-update? (fn [] true))
                           still-to-initiate-hover-time (cond (number? still-to-initiate-hover)
                                                              still-to-initiate-hover

                                                              (= still-to-initiate-hover true)
                                                              100

                                                              :else
                                                              0)
                           state-atom (atom {:hover-values     (reduce (fn [a input-key]
                                                                         (assoc a input-key nil))
                                                                       {}
                                                                       input-keys)
                                             :still            false
                                             :still-timeout-id nil})
                           on-still-timeout (fn []
                                              (when (should-update?)
                                                (swap! state-atom assoc :still true :still-timeout-id nil)))
                           mouse-position-instance (when still-to-initiate-hover
                                                     ((:get-instance mouse-position-input-definition)
                                                       {:on-state-changed (fn []
                                                                            (let [state (deref state-atom)]
                                                                              (when-let [still-timeout-id (:still-timeout-id state)]
                                                                                (js/clearTimeout still-timeout-id))
                                                                              (when (and (anyting-hovered? state)
                                                                                         (or still-to-remain-hovered
                                                                                             (not (:still state)))
                                                                                         (should-update?))
                                                                                (swap! state-atom assoc
                                                                                       :still false
                                                                                       :still-timeout-id (js/setTimeout on-still-timeout
                                                                                                                        still-to-initiate-hover-time)))))}))]
                       (add-watch state-atom :state-change-notifier (fn [_ _ old-state new-state]
                                                                      (when (not= (get-input old-state keys still-to-initiate-hover)
                                                                                  (get-input new-state keys still-to-initiate-hover))
                                                                        (on-state-changed))))
                       {:ready?                     (fn [] true)
                        :get-input                  (fn [] (get-input (deref state-atom) keys still-to-initiate-hover))
                        :element-attribute-modifier (fn [{attributes :attributes}]
                                                      (when-let [match (some (fn [{update :update :as key}]
                                                                               (when (contains? attributes update)
                                                                                 key))
                                                                             keys)]
                                                        (let [reset-hover! (fn [match]
                                                                             (swap! state-atom (fn [state]
                                                                                                 (-> state
                                                                                                     (assoc-in [:hover-values (:input match)] nil)
                                                                                                     (assoc :still false)
                                                                                                     (assoc :hovered-element-id nil)))))]
                                                          (-> attributes
                                                              (dissoc (:update match))
                                                              (add-event-handler :on-mouse-enter (fn [event]
                                                                                                   (let [element-id (or (aget event "target" "_onyxiaElementHoveredId")
                                                                                                                        (do (let [element-id (random-uuid)]
                                                                                                                              (aset event "target" "_onyxiaElementHoveredId" element-id)
                                                                                                                              element-id)))]
                                                                                                     (when (should-update?)
                                                                                                       (swap! state-atom (fn [state]
                                                                                                                           (-> state
                                                                                                                               (assoc-in [:hover-values (:input match)] ((:update match) attributes))
                                                                                                                               (assoc :still false)
                                                                                                                               (assoc :hovered-element-id element-id))))))))
                                                              (add-event-handler :on-mouse-leave (fn [event]
                                                                                                   (when (should-update?)
                                                                                                     (reset-hover! match))))
                                                              (add-event-handler :on-will-unmount (fn [{element :element}]
                                                                                                    (when (should-update?)
                                                                                                      (when-let [element-id (aget element "_onyxiaElementHoveredId")]
                                                                                                        (when (= (:hovered-element-id (deref state-atom))
                                                                                                                 element-id)
                                                                                                          (reset-hover! match))))))))))
                        :will-unmount               (fn [args]
                                                      (when mouse-position-instance
                                                        ((:will-unmount mouse-position-instance) args)))})))})
