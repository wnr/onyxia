(ns onyxia.input.element-hovered
  (:require [onyxia.attributes :refer [add-event-handler]]))

(defn get-definition
  []
  {:name         "element_hovered"
   :get-instance (fn [{on-state-changed :on-state-changed}]
                   (let [state-atom (atom {:hover nil})]
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (:hover (deref state-atom)))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [id (:element-hovered-value attributes)]
                                                      (-> attributes
                                                          (dissoc :element-hovered-value)
                                                          (add-event-handler :on-mouse-enter (fn []
                                                                                               (swap! state-atom assoc :hover id)
                                                                                               (on-state-changed)))
                                                          (add-event-handler :on-mouse-leave (fn []
                                                                                               (swap! state-atom assoc :hover nil)
                                                                                               (on-state-changed))))))}))})
