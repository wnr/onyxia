(ns onyxia.vdom
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is= is is-not]]))

(defn- clean-element-sequence
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
                 []))}
  [seq]
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

(defn view?
  {:test (fn []
           (is (view? {:name "view-definition"}))
           (is (view? [{:name "view-definition"}]))
           (is (view? [{:name "view-definition"} {:foo :bar} "child"]))
           (is-not (view? [:div])))}
  [vdom-element]
  (or (map? (first vdom-element)) (map? vdom-element)))

(defn get-view-definition
  {:test (fn []
           (is= (get-view-definition {:name "hello"})
                {:name "hello"})
           (is= (get-view-definition [{:name "hello"}])
                {:name "hello"}))}
  [view]
  (if (map? view)
    view
    (first view)))

(defn get-view-input
  {:test (fn []
           (is= (get-view-input {})
                {})
           (is= (get-view-input [{}])
                {})
           (is= (get-view-input [{} {}])
                {})
           (is= (get-view-input [{} {:foo :bar}])
                {:foo :bar})
           (is= (get-view-input [{} {:foo :bar} [:div] [:span]])
                {:foo      :bar
                 :children [[:div {:key "0"}] [:span {:key "1"}]]})
           (is= (get-view-input [{} [:div] [:span]])
                {:children [[:div {:key "0"}] [:span {:key "1"}]]})
           (is= (get-view-input [{} [:div] [:span]])
                {:children [[:div {:key "0"}] [:span {:key "1"}]]})
           (is= (get-view-input [{} "foo"])
                {:children ["foo"]})
           (is= (get-view-input [{} [{:a "a"}]])
                {:children [[{:a "a"} {:key "0"}]]})
           )}
  [view]
  (let [attributes (second view)]
    (let [has-attributes? (map? attributes)]
      (merge (if has-attributes?
               attributes
               {})
             (let [children (nthrest view (if has-attributes? 2 1))]
               (when (not (empty? children))
                 ;; TODO: What about conflicts with existing input called "children"?
                 {:children (map-indexed (fn [index child]
                                           (if (or (keyword? (first child))
                                                   (map? (first child)))
                                             (let [child (if (map? (second child))
                                                           child
                                                           (concat [(first child) {}] (rest child)))]
                                               (assoc-in (into [] child) [1 :key] (str index)))
                                             child))
                                         children)}))))))