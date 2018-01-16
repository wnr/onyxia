(ns onyxia.dom-operator
  (:require [ysera.error :refer [error]]))

(def pending-operations-atom (atom {:status    :idle
                                    :read-dom  []
                                    :write-dom []}))

(defn- execute-operations-seq! [seq]
  (reduce (fn [_ operation]
            ((:execute! operation)))
          nil
          seq))

(defn execute-pending-operations! []
  (let [pending-operations @pending-operations-atom]
    (reset! pending-operations-atom {:status    :idle
                                     :read-dom  []
                                     :write-dom []})
    (let [read-operations (:read-dom pending-operations)
          write-operations (:write-dom pending-operations)]
      (execute-operations-seq! read-operations)
      (execute-operations-seq! write-operations))))
(defn queue-pending-operations-execution! []
  (when (not= (:status @pending-operations-atom) :idle)
    (error "Cannot queue unless status idle."))
  (swap! pending-operations-atom assoc :status :execution-queued)
  (js/requestAnimationFrame execute-pending-operations!))

(defn add-pending-operation! [operation]
  (let [status (:status (swap! pending-operations-atom (fn [pending-operations]
                                                         (update pending-operations (:operation operation) conj operation))))]
    (when (= status :idle)
      (queue-pending-operations-execution!))))
