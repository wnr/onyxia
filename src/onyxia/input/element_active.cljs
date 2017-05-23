(ns onyxia.input.element-active
  (:require [onyxia.attributes :refer [add-event-handler]]))

(def definition
  {:name         "element-active"
   :get-instance (fn [{on-state-changed :on-state-changed
                       should-update?   :should-update?}]
                   (let [should-update? (or should-update? (fn [] true))
                         state-atom (atom {:active nil})]
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (:active-id (deref state-atom)))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [element-active-id (:element-active-value attributes)]
                                                      (-> attributes
                                                          (dissoc :element-active-value)
                                                          (add-event-handler :on-mouse-down (fn []
                                                                                              (when (should-update?)
                                                                                                (swap! state-atom assoc :active-id element-active-id)
                                                                                                (on-state-changed))))
                                                          (add-event-handler :on-mouse-up (fn []
                                                                                            (when (should-update?)
                                                                                              (swap! state-atom assoc :active-id nil)
                                                                                              (on-state-changed))))
                                                          (add-event-handler :on-mouse-leave (fn []
                                                                                               (when (and (should-update?)
                                                                                                          (:element-active-value (deref state-atom)))
                                                                                                 (swap! state-atom assoc :active-id nil)
                                                                                                 (on-state-changed)))))))}))})