(ns onyxia.vdom
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]))

(defn-
  ^{:test (fn []
            (is= (clean-children
                  [[:div] [:div]])
                 [[:div] [:div]])
            (is= (clean-children
                  [[:div] []])
                 [[:div] nil])
            (is= (clean-children
                  [[]])
                 [nil])
            (is= (clean-children
                  [[[:div] [:div]]])
                 [[:div] [:div]])
            (is= (clean-children
                  [[[:a] [:b]] [[:c] [:d]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (clean-children
                  [[[[:a] [:b]] [[:c] [:d]]]])
                 [[:a] [:b] [:c] [:d]])
            (is= (clean-children
                  [[:p] [[:a] [:b]]])
                 [[:p] [:a] [:b]])
            (is= (clean-children
                  ["Hello"])
                 ["Hello"])
            (is= (clean-children
                  [0])
                 [0])
            ;; React.createElement cares about children ordering, so it is important to keep nil children.
            ;; Otherwise, React won't be able to recognize unaffected children (so it will re-mount all of them).
            ;; TODO: Should nil children become a part of the Onyxia API?
            (is= (clean-children
                  [nil [:div] nil])
                 [nil [:div] nil]))}
  clean-children [seq]
  (->> (reduce (fn [result entry]
                 (if (not (sequential? entry))
                   (conj result entry)
                   (if (sequential? (first entry))
                     (concat result (clean-children entry))
                     (conj result entry))))
               []
               seq)
       (map (fn [value]
              (if (or (nil? value)
                      (and (sequential? value) (empty? value)))
                nil
                value)))))

(defn
  ^{:test (fn []
            (is= (formalize-element
                  [:div])
                 [:div {} []])
            (is= (formalize-element
                  [:div []])
                 [:div {} [nil]])
            (is= (formalize-element
                  [:div {:attr true}])
                 [:div {:attr true} []])
            (is= (formalize-element
                  [:div [:button] [:button]])
                 [:div {} [[:button] [:button]]])
            (is= (formalize-element
                  [:div [[:button] [:button]]])
                 [:div {} [[:button] [:button]]])
            (is= (formalize-element
                  [:div {:attr true} [:button]])
                 [:div {:attr true} [[:button]]])
            (is= (formalize-element
                  [:div {:attr true} [[:button]]])
                 [:div {:attr true} [[:button]]])
            (is= (formalize-element
                  [:h1 "Hello"])
                 [:h1 {} ["Hello"]])
            (is= (formalize-element
                  [:h1 0])
                 [:h1 {} [0]])
            (is= (formalize-element
                  [:div [:p] [[:a] [:b]]])
                 [:div {} [[:p] [:a] [:b]]]))}
    formalize-element [element]
    (if (map? (second element))
      [(first element) (second element) (clean-children (nthrest element 2))]
      [(first element) {} (clean-children (nthrest element 1))]))
