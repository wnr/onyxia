(ns onyxia.vdom
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is= is is-not]]))

(defn-
  ^{:test (fn []
            (is= (flatten-element-sequence
                  [[:div] [:div]])
                 [[:div] [:div]])
            (is= (flatten-element-sequence
                  [[:div] []])
                 [[:div] nil])
            (is= (flatten-element-sequence
                  [[]])
                 [nil])
            (is= (flatten-element-sequence
                  [[[:div] [:div]]])
                 [[:div] [:div]])
            (is= (flatten-element-sequence
                  [[[:a] [:b]] [[:c] [:d]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (flatten-element-sequence
                  [[[[:a] [:b]] [[:c] [:d]]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (flatten-element-sequence
                  [[:p] [[:a] [:b]]])
                 [[:p] [:a] [:b]])
            (is= (flatten-element-sequence
                  ["Hello"])
                 ["Hello"])
            (is= (flatten-element-sequence
                  [0])
                 [0])
            ;; React.createElement cares about children ordering, so it is important to keep nil children.
            ;; Otherwise, React won't be able to recognize unaffected children (so it will re-mount all of them).
            ;; TODO: Should nil children become a part of the Onyxia API?
            (is= (flatten-element-sequence
                  [nil [:div] nil])
                 [nil [:div] nil]))}
  flatten-element-sequence [seq]
  (->> (reduce (fn [result entry]
                 (if (not (sequential? entry))
                   (conj result entry)
                   (if (sequential? (first entry))
                     (concat result (flatten-element-sequence entry))
                     (conj result entry))))
               []
               seq)
       (map (fn [value]
              (if (or (nil? value)
                      (and (sequential? value) (empty? value)))
                nil
                value)))))

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
