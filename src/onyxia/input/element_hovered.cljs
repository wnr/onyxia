(ns onyxia.input.element-hovered)

(defn get-definition
  []
  {:name         "element_hovered"
   :get-instance (fn [{on-state-changed :on-state-changed}]
                   (let [state-atom (atom {:hover nil})]
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (:hover (deref state-atom)))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [id (:element-hover-id attributes)]
                                                      (-> attributes
                                                          (dissoc :element-hover-id)
                                                          (assoc :on-mouse-enter (fn []
                                                                                   (swap! state-atom assoc :hover id)
                                                                                   (on-state-changed)))
                                                          (assoc :on-mouse-leave (fn []
                                                                                   (swap! state-atom assoc :hover nil)
                                                                                   (on-state-changed))))))}))})
