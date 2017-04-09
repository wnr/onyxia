(ns onyxia.react-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [camel-snake-kebab.core :refer [->camelCase]]))

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
