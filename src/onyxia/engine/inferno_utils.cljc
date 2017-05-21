(ns onyxia.engine.inferno-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [camel-snake-kebab.core :refer [->camelCase]]
            [onyxia.attributes-utils :refer [replace-key replace-value change-attribute]]
            [onyxia.engine.react-utils :as react-utils]))

(defn style->inferno-style
  [style]
  (react-utils/style->react-style style))

(defn formalize-event-handlers
  {:test (fn []
           (is= (formalize-event-handlers nil) [])
           (is= (formalize-event-handlers []) [])
           (is= (formalize-event-handlers [:foo]) [[:foo]])
           (is= (formalize-event-handlers [{} :foo]) [[{} :foo]])
           (is= (formalize-event-handlers +) [+])
           (is= (formalize-event-handlers [[:foo]]) [[:foo]])
           (is= (formalize-event-handlers [[{} :foo]]) [[{} :foo]])
           (is= (formalize-event-handlers [+]) [+]))}
  [handlers]
  (if (or (and (coll? handlers)
               (not (empty? handlers))
               (or (map? (first handlers))
                   (keyword? (first handlers))))
          (and (not (coll? handlers))
               (not (nil? handlers))))
    [handlers]
    (or handlers [])))

(defn
  ^{:test (fn []
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
  map-to-inferno-attributes [attrs {on-dom-event :on-dom-event}]
  (let [handle-dom-event (fn [{attributes-key :attributes-key
                               type           :type}]
                           (on-dom-event {:type      type
                                          :dom-event :event
                                          :handlers  (formalize-event-handlers (get attrs attributes-key))}))]
    (-> attrs
        (change-attribute {:key :class :new-key :className})
        (change-attribute {:key :on-click :new-key :onClick :assoc (fn [_] (handle-dom-event {:attributes-key :on-click :type :on-click}))})
        (change-attribute {:key :on-mouse-enter :new-key :onMouseEnter :assoc (fn [_] (handle-dom-event {:attributes-key :on-mouse-enter :type :on-mouse-enter}))})
        (change-attribute {:key :on-mouse-leave :new-key :onMouseLeave :assoc (fn [_] (handle-dom-event {:attributes-key :on-mouse-leave :type :on-mouse-leave}))})
        (change-attribute {:key :on-mouse-up :new-key :onMouseUp :assoc (fn [_] (handle-dom-event {:attributes-key :on-mouse-up :type :on-mouse-up}))})
        (change-attribute {:key :on-mouse-down :new-key :onMouseDown :assoc (fn [_] (handle-dom-event {:attributes-key :on-mouse-down :type :on-mouse-down}))})
        (change-attribute {:key :style :update style->inferno-style}))))


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
