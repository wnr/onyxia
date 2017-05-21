(ns onyxia.input.element-active
  (:require [onyxia.attributes :refer [add-event-handler]]))

(def definition
  {:name         "element_active"
   :get-instance (fn [{on-state-changed :on-state-changed}]
                   (let [state-atom (atom {:active nil})]
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (:active-id (deref state-atom)))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [element-active-id (:element-active-value attributes)]
                                                      (-> attributes
                                                          (dissoc :element-active-value)
                                                          (add-event-handler :on-mouse-down (fn []
                                                                                              (swap! state-atom assoc :active-id element-active-id)
                                                                                              (on-state-changed)))
                                                          (add-event-handler :on-mouse-up (fn []
                                                                                            (swap! state-atom assoc :active-id nil)
                                                                                            (on-state-changed)))
                                                          (add-event-handler :on-mouse-leave (fn []
                                                                                               (when (:element-active-value (deref state-atom))
                                                                                                 (swap! state-atom assoc :active-id nil)
                                                                                                 (on-state-changed)))))))}))})