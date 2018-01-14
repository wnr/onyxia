(ns onyxia.engine.inferno-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [onyxia.attributes-map :refer [kebab->camel]]
            [onyxia.attributes-utils :refer [replace-key replace-value change-attribute map-default-attribute-events formalize-event-handlers]]))

(defn style->inferno-style
  "Maps a given style map to a inferno-flavored style map (with camelcased keys, etc.)."
  [style]
  (reduce (fn [inferno-style [key value]]
            (let [string-value (if (number? value)
                          (str value)
                          (name value))
                  inferno-key (kebab->camel key)]
              (assoc inferno-style inferno-key string-value)))
          {}
          style))


(defn map-attribute-events [attrs {on-dom-event :on-dom-event}]
  (-> attrs
      (map-default-attribute-events {:on-dom-event on-dom-event :attribute-keys [
                                                                                 :on-blur
                                                                                 :on-change
                                                                                 :on-click
                                                                                 :on-focus
                                                                                 :on-input
                                                                                 :on-mouse-down
                                                                                 :on-mouse-enter
                                                                                 :on-mouse-leave
                                                                                 :on-mouse-up
                                                                                 :on-key-down
                                                                                 ]})
      ;(change-attribute {:key     :on-xxx
      ;                   :new-key :onYyy
      ;                   :assoc   (fn [e]
      ;                              (handle-dom-event {:attributes-key :on-xxx :type :on-xxx :event e}))}))
      ))

(defn map-to-inferno-attributes
  {:test (fn []
           ;; Keep unknown attributes unchanged.
           (is= (map-to-inferno-attributes {:unknown "test"} {})
                {:unknown "test"})
           ;; :class -> :className
           (is= (map-to-inferno-attributes {:class "foo bar"} {})
                {:className "foo bar"})
           ;; :style -> :style (with values changed to inferno-flavor).
           (is= (map-to-inferno-attributes {:style {"padding-left" ""}} {})
                {:style {"paddingLeft" ""}})
           ;; Map special SVG attributes
           (is= (map-to-inferno-attributes {:text-anchor "foo" :xlink:href "bar"} {})
                {"textAnchor" "foo" "xlinkHref" "bar"}))}
  [attrs args]
  (-> attrs
      ;(change-attribute {:key :class :new-key :className})
      ;(change-attribute {:key :for :new-key :htmlFor})
      ;(change-attribute {:key :charset :new-key :charSet})
      (map-attribute-events args)
      ;; Seems like Inferno supports snake-case styles. Or does it just pass it along to the DOM and modern browsers just happen to support it?
      ;; anyway, should not do this very expensive operation if not really needed (old browsers)...
      ;(change-attribute {:key :style :update style->inferno-style})
      ))
