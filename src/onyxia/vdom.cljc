(ns onyxia.vdom
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [camel-snake-kebab.core :refer [->camelCase]]))

(defn-
  ^{:test (fn []
            (is= (clean-children
                  [[:div] [:div]])
                 [[:div] [:div]])
            (is= (clean-children
                  [[:div] []])
                 [[:div]])
            (is= (clean-children
                  [[]])
                 [])
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
                 [0]))}
  clean-children [seq]
  (remove (fn [value]
            (or (nil? value)
                (and (sequential? value) (empty? value))))
          (reduce (fn [result entry]
                    (if (not (sequential? entry))
                      (conj result entry)
                      (if (sequential? (first entry))
                        (concat result (clean-children entry))
                        (conj result entry))))
                  []
                  seq)))

(defn
  ^{:test (fn []
            (is= (formalize-element
                  [:div])
                 [:div {} []])
            (is= (formalize-element
                  [:div []])
                 [:div {} []])
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

(defn style->react-style
  "Maps a given style map to a react-flavored style map (with camedlcased keys, etc.).
   See https://facebook.github.io/react/docs/dom-elements.html#style"
  {:test (fn []
           (is= (style->react-style {})
                {})
           (is= (style->react-style {"position" "relative"})
                {"position" "relative"})
           (is= (style->react-style {"top" 0})
                {"top" "0"})
           (is= (style->react-style {"padding-left" "1px solid black"})
                {"paddingLeft" "1px solid black"}))}
  [style]
  (reduce (fn [react-style [key value]]
            (let [key (name key)
                  value (if (number? value)
                          (str value)
                          (name value))
                  react-key (condp re-seq key
                              ;; single word keys such as "position", "left", etc.
                              #"^\w+$" key

                              ;; snake-cased non-prefixed multiword keys such as "padding-left", "align-items", etc.
                              #"^\w+(\-\w+)+$" (->camelCase key)

                              (error "Style property " key " not implemented."))]
              (assoc react-style react-key value)))
          {}
          style))

(defn
  ^{:test (fn []
            ;; Keep unknown attributes unchanged.
            (is= (map-to-react-attributes {:unknown "test"} {})
                 {:unknown "test"})
            ;; :class -> :className
            (is= (map-to-react-attributes {:class "foo bar"} {})
                 {:className "foo bar"})
            ;; :style -> :style (with values changed to react-flavor).
            (is= (map-to-react-attributes {:style {"padding-left" ""}} {})
                 {:style {"paddingLeft" ""}}))}
  map-to-react-attributes [attrs {on-dom-event :on-dom-event}]
  (let [replace-key (fn [map key new-key value]
                      (if (contains? map key)
                        (-> (dissoc map key)
                            (assoc new-key value))
                        map))
        replace-value (fn [map key value]
                        (if (contains? map key)
                          (update map key value)
                          map))]
    (-> attrs
        (replace-key :class :className (:class attrs))
        (replace-value :style style->react-style)
        (replace-key :on-click :onClick (fn [event] (on-dom-event {:type               :on-click
                                                                   :dom-event :event
                                                                   :data      (:on-click attrs)}))))))
