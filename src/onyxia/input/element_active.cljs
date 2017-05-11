(ns onyxia.input.element-active)

(defn get-definition
  []
  {:name         "element_active"
   :get-instance (fn [{on-state-changed :on-state-changed}]
                   (let [state-atom (atom {:active nil})]
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (:active-id (deref state-atom)))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [element-active-id (:element-active-id attributes)]
                                                      (-> attributes
                                                          (dissoc :element-active-id)
                                                          (assoc :on-mouse-down (fn []
                                                                                  (swap! state-atom assoc :active-id element-active-id)
                                                                                  (on-state-changed)))
                                                          (assoc :on-mouse-up (fn []
                                                                                (swap! state-atom assoc :active-id nil)
                                                                                (on-state-changed)))
                                                          (assoc :on-mouse-leave (fn []
                                                                                   (when (:element-active-id (deref state-atom))
                                                                                     (swap! state-atom assoc :active-id nil)
                                                                                     (on-state-changed)))))))}))})