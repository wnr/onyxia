(ns onyxia.vdom
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is= is is-not]]))

(defn-
  ^{:test (fn []
            (is= (clean-element-sequence
                   [[:div] [:div]])
                 [[:div] [:div]])
            (is= (clean-element-sequence
                   [[[:div] [:div]]])
                 [[:div] [:div]])
            (is= (clean-element-sequence
                   [[[:a] [:b]] [[:c] [:d]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (clean-element-sequence
                   [[[[:a] [:b]] [[:c] [:d]]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (clean-element-sequence
                   [[:p] [[:a] [:b]]])
                 [[:p] [:a] [:b]])
            (is= (clean-element-sequence
                   ["Hello"])
                 ["Hello"])
            (is= (clean-element-sequence
                   [0])
                 [0])
            ;; For element sequences, we want to remove empty value (empty list and nil) values.
            (is= (clean-element-sequence
                   [nil [:div] nil])
                 [[:div]])
            (is= (clean-element-sequence
                   [[:div] []])
                 [[:div]])
            (is= (clean-element-sequence
                   [[]])
                 [nil]))}
  clean-element-sequence [seq]
  (->> (reduce (fn [result entry]
                 (if (not (sequential? entry))
                   (conj result entry)
                   (if (sequential? (first entry))
                     (concat result (clean-element-sequence entry))
                     (conj result entry))))
               []
               seq)
       (remove (fn [value]
                 (or (nil? value)
                     (and (sequential? value) (empty? value)))))))

(defn ensure-attributes-map
  {:test (fn []
           (is= (ensure-attributes-map
                  [:div])
                [:div {}])
           (is= (ensure-attributes-map
                  [:div {}])
                [:div {}])
           (is= (ensure-attributes-map
                  [:div 1 2 3])
                [:div {} 1 2 3]))}
  [element]
  (if (map? (second element))
    element
    (vec (concat [(first element) {}]
                 (nthrest element 1)))))

(defn formalize-element
  ^{:test (fn []
            (is= (formalize-element
                   [:div])
                 [:div {} []])
            (is= (formalize-element
                   [:div []])
                 [:div {} [[]]])
            (is= (formalize-element
                   [:div {:attr true}])
                 [:div {:attr true} []])
            (is= (formalize-element
                   [:div [:button] [:button]])
                 [:div {} [[:button] [:button]]])
            (is= (formalize-element
                   [:div [[:button] [:button]]])
                 [:div {} [[[:button] [:button]]]])
            (is= (formalize-element
                   [:div {:attr true} [:button]])
                 [:div {:attr true} [[:button]]])
            (is= (formalize-element
                   [:div {:attr true} [[:button]]])
                 [:div {:attr true} [[[:button]]]])
            (is= (formalize-element
                   [:h1 "Hello"])
                 [:h1 {} ["Hello"]])
            (is= (formalize-element
                   [:h1 0])
                 [:h1 {} [0]])
            (is= (formalize-element
                   [:div [:p] [[:a] [:b]]])
                 [:div {} [[:p] [[:a] [:b]]]]))}
  [element]
  (if (map? (second element))
    [(first element) (second element) (nthrest element 2)]
    [(first element) {} (nthrest element 1)]))

(defn element?
  {:test (fn []
           (is (element? [:div]))
           (is-not (element? "hello"))
           (is-not (element? ["hello"])))}
  [node]
  (keyword? (first node)))

(defn element-sequence?
  {:test (fn []
           (is (element-sequence? [[:a] [:b]]))
           (is (element-sequence? [[:div]]))
           (is (element-sequence? []))
           (is (element-sequence? ["test"]))
           (is (element-sequence? [0]))
           (is (element-sequence? [nil]))
           (is-not (element-sequence? [:div]))
           (is-not (element-sequence? "test")))}
  [node]
  (and (not (element? node))
       (not (string? node))
       (sequential? node)))
