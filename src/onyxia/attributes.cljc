(ns onyxia.attributes
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is= is is-not]]))

;(defn event-handler?
;  {:test (fn []
;           (is (event-handler? (fn [])))
;           (is (event-handler? [:x]))
;           (is (event-handler? [{}  :x]))
;           (is-not (event-handler? []))
;           (is-not (event-handler? [[:x]]))
;           (is-not (event-handler? [[(fn [])]])))}
;  [event-handler]
;  (or )
;  (and (not (empty? event-handler))
;       (or (map? (first event-handler))
;           (not (coll? (first event-handler))))))

(defn add-event-handler
  {:test (fn []
           (is= (add-event-handler {} :on-click :x) {:on-click [:x]})
           (is= (add-event-handler {:on-click [:x :y]} :on-click :z) {:on-click [:x :y :z]}))}
  [attributes key event-handler]
  (if-let [existing-value (get attributes key)]
    (update attributes key conj event-handler)
    (assoc attributes key [event-handler])))