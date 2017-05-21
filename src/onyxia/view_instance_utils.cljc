(ns onyxia.view-instance-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]))

(defn formalize-input-definitions
  {:test (fn []
           (is= (formalize-input-definitions {"foo" {:name "foo"}
                                              "bar" [{:name "bar"}]
                                              "xar" [{:name "xar"} {:a "a"}]})
                {"foo" [{:name "foo"} {}]
                 "bar" [{:name "bar"} {}]
                 "xar" [{:name "xar"} {:a "a"}]}))}
  [input-definiitions]
  (reduce (fn [output key]
            (let [value (get output key)]
              (assoc output key (if (map? value)
                                  [value {}]
                                  [(first value) (or (second value) {})]))))
          input-definiitions
          (keys input-definiitions)))

(defn formalize-output-definitions
  [output-definitions]
  (formalize-input-definitions output-definitions))