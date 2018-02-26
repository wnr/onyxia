(ns onyxia.input.my-position
  (:require [ysera.error :refer [error]]
            [onyxia.dom-operator :refer [add-pending-operation!]]))

(defn get-input-value
  [state]
  (select-keys state [:x :y]))

(def definition
  {:name         "my-position"
   :get-instance (fn [{on-state-changed :on-state-changed
                       should-update?   :should-update?
                       input-key        :input-key}]
                   (let [should-update? (or should-update? (fn [] true))
                         state-atom (atom {:x nil :y nil})
                         on-position-change-listener (fn [{x :x y :y}]
                                                       (when (and (should-update?)
                                                                  (or (not= x (:x @state-atom))
                                                                      (not= y (:y @state-atom))))
                                                         (swap! state-atom (fn [state]
                                                                             (merge state {:x x
                                                                                           :y y})))
                                                         (on-state-changed)))]
                     {:ready?       (fn []
                                      (not (nil? (get-input-value @state-atom))))
                      :get-input    (fn []
                                      {input-key (get-input-value @state-atom)})
                      :did-mount    (fn [{element :element}]
                                      (let [read-position! (fn []
                                                             (add-pending-operation! {:operation :read-dom
                                                                                      :execute!  (fn []
                                                                                                   (on-position-change-listener (let [dom-rect (.getBoundingClientRect element)]
                                                                                                                                  {:x (.-x dom-rect)
                                                                                                                                   :y (.-y dom-rect)})))}))]
                                        (swap! state-atom assoc :interval-fn-id (js/setInterval read-position!
                                                                                                1000))
                                        (read-position!)))
                      :will-unmount (fn []
                                      (js/clearTimeout (:interval-fn-id (deref state-atom))))}))})
