(ns onyxia.input.animation
  (:require [onyxia.dom-operator :refer [add-pending-operation!]]))

(def definition
  {:name         "animation"
   :get-instance (fn [{on-state-changed :on-state-changed
                       should-update?   :should-update?}]
                   (let [should-update? (or should-update? (fn [] true))
                         state-atom (atom {:tick             0
                                           :last-tick        nil
                                           :last-render-time nil})]
                     {:ready?     (fn [] true)
                      :get-input  (fn []
                                    (let [{tick             :tick
                                           last-render-time :last-render-time} (deref state-atom)]
                                      {:tick          tick
                                       :event         (if (= tick 0)
                                                        :start
                                                        :step)
                                       :delta-time-ms (if (= tick 0)
                                                        nil
                                                        (- (js/Date.now) last-render-time))}))
                      :did-render (fn []
                                    (when (not (:operation-requested (deref state-atom)))
                                      (add-pending-operation!
                                        {:operation :write-dom
                                         :execute!  (fn []
                                                      (swap! state-atom assoc :operation-requested false)
                                                      (on-state-changed))}))
                                    (swap! state-atom (fn [state]
                                                        (-> state
                                                            (assoc :last-render-time (js/Date.now)
                                                                   :operation-requested true)
                                                            (update :tick inc)))))}))})