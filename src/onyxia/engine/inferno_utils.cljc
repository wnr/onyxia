(ns onyxia.engine.inferno-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [camel-snake-kebab.core :refer [->camelCase]]
            [onyxia.attributes-utils :refer [replace-key replace-value change-attribute map-default-attribute-events formalize-event-handlers]]
            [onyxia.engine.react-utils :as react-utils]))

(defn style->inferno-style
  [style]
  (react-utils/style->react-style style))

;(defn formalize-event-handlers
;  {:test (fn []
;           (is= (formalize-event-handlers nil) [])
;           (is= (formalize-event-handlers []) [])
;           (is= (formalize-event-handlers [:foo]) [[:foo]])
;           (is= (formalize-event-handlers [{} :foo]) [[{} :foo]])
;           (is= (formalize-event-handlers +) [+])
;           (is= (formalize-event-handlers [[:foo]]) [[:foo]])
;           (is= (formalize-event-handlers [[{} :foo]]) [[{} :foo]])
;           (is= (formalize-event-handlers [+]) [+]))}
;  [handlers]
;  (if (or (and (coll? handlers)
;               (not (empty? handlers))
;               (or (map? (first handlers))
;                   (keyword? (first handlers))))
;          (and (not (coll? handlers))
;               (not (nil? handlers))))
;    [handlers]
;    (or handlers [])))

(defn map-attribute-events [attrs {on-dom-event :on-dom-event}]
  (let [handle-dom-event (fn [{attributes-key :attributes-key
                               type           :type
                               event          :event}]
                           (on-dom-event {:type      type
                                          :dom-event :event
                                          :event     event
                                          :handlers  (formalize-event-handlers (get attrs attributes-key))}))]
    (-> attrs
        (map-default-attribute-events {:on-dom-event on-dom-event :attribute-keys [
                                                                                   :on-change
                                                                                   :on-click
                                                                                   :on-input
                                                                                   :on-mouse-down
                                                                                   :on-mouse-enter
                                                                                   :on-mouse-leave
                                                                                   :on-mouse-up
                                                                                   ]})
        ;(change-attribute {:key     :on-xxx
        ;                   :new-key :onYyy
        ;                   :assoc   (fn [e]
        ;                              (handle-dom-event {:attributes-key :on-xxx :type :on-xxx :event e}))}))
    )))

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
  [attrs {on-dom-event :on-dom-event :as args}]
  (-> attrs
      (change-attribute {:key :class :new-key :className})
      (map-attribute-events args)
      (change-attribute {:key :style :update style->inferno-style})))


(defn add-key-attribute
  {:test (fn []
           ;; Should add key if present in system options, and not in element attrs.
           (is= (add-key-attribute [:div {}] "key-value")
                [:div {:key "key-value"}])
           ;; Should not override existing key attribute.
           (is= (map-to-inferno-attributes {:key "b"} {:key "a"})
                {:key "b"}))}
  [vdom-element key-value]
  (update vdom-element 1 (fn [attrs]
                           (if (contains? attrs :key)
                             attrs
                             (assoc attrs :key key-value)))))
