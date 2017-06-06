(ns onyxia.engine.react-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
            [ysera.error :refer [error]]
            [camel-snake-kebab.core :refer [->camelCase]]
            [onyxia.attributes-utils :refer [replace-key replace-value change-attribute]]))

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

; Taken from https://github.com/facebook/react/blob/b1b4a2fb252f26fe10d29ba60d85ff89a85ff3ec/src/renderers/dom/shared/SVGDOMPropertyConfig.js
(def svg-attributes-map
  {"accent-height"                "accentHeight"
   "alignment-baseline"           "alignmentBaseline"
   "arabic-form"                  "arabicForm"
   "baseline-shift"               "baselineShift"
   "cap-height"                   "capHeight"
   "clip-path"                    "clipPath"
   "clip-rule"                    "clipRule"
   "color-interpolation"          "colorInterpolation"
   "color-interpolation-filters"  "colorInterpolationFilters"
   "color-profile"                "colorProfile"
   "color-rendering"              "colorRendering"
   "dominant-baseline"            "dominantBaseline"
   "enable-background"            "enableBackground"
   "fill-opacity"                 "fillOpacity"
   "fill-rule"                    "fillRule"
   "flood-color"                  "floodColor"
   "flood-opacity"                "floodOpacity"
   "font-family"                  "fontFamily"
   "font-size"                    "fontSize"
   "font-size-adjust"             "fontSizeAdjust"
   "font-stretch"                 "fontStretch"
   "font-style"                   "fontStyle"
   "font-variant"                 "fontVariant"
   "font-weight"                  "fontWeight"
   "glyph-name"                   "glyphName"
   "glyph-orientation-horizontal" "glyphOrientationHorizontal"
   "glyph-orientation-vertical"   "glyphOrientationVertical"
   "horiz-adv-x"                  "horizAdvX"
   "horiz-origin-x"               "horizOriginX"
   "image-rendering"              "imageRendering"
   "letter-spacing"               "letterSpacing"
   "lighting-color"               "lightingColor"
   "marker-end"                   "markerEnd"
   "marker-mid"                   "markerMid"
   "overline-position"            "overlinePosition"
   "overline-thickness"           "overlineThickness"
   "paint-order"                  "paintOrder"
   "panose-1"                     "panose1"
   "pointer-events"               "pointerEvents"
   "rendering-intent"             "renderingIntent"
   "shape-rendering"              "shapeRendering"
   "stop-color"                   "stopColor"
   "stop-opacity"                 "stopOpacity"
   "strikethrough-position"       "strikethroughPosition"
   "strikethrough-thickness"      "strikethroughThickness"
   "stroke-dasharray"             "strokeDasharray"
   "stroke-dashoffset"            "strokeDashoffset"
   "stroke-linecap"               "strokeLinecap"
   "stroke-linejoin"              "strokeLinejoin"
   "stroke-miterlimit"            "strokeMiterlimit"
   "stroke-opacity"               "strokeOpacity"
   "stroke-width"                 "strokeWidth"
   "text-anchor"                  "textAnchor"
   "text-decoration"              "textDecoration"
   "text-rendering"               "textRendering"
   "underline-position"           "underlinePosition"
   "underline-thickness"          "underlineThickness"
   "unicode-bidi"                 "unicodeBidi"
   "unicode-range"                "unicodeRange"
   "units-per-em"                 "unitsPerEm"
   "v-alphabetic"                 "vAlphabetic"
   "v-hanging"                    "vHanging"
   "v-ideographic"                "vIdeographic"
   "v-mathematical"               "vMathematical"
   "vector-effect"                "vectorEffect"
   "vert-adv-y"                   "vertAdvY"
   "vert-origin-x"                "vertOriginX"
   "vert-origin-y"                "vertOriginY"
   "word-spacing"                 "wordSpacing"
   "writing-mode"                 "writingMode"
   "x-height"                     "xHeight"
   "xlink:actuate"                "xlinkActuate"
   "xlink:arcrole"                "xlinkArcrole"
   "xlink:href"                   "xlinkHref"
   "xlink:role"                   "xlinkRole"
   "xlink:show"                   "xlinkShow"
   "xlink:title"                  "xlinkTitle"
   "xlink:type"                   "xlinkType"
   "xml:base"                     "xmlBase"
   "xmlns:xlink"                  "xmlnsXlink"
   "xml:lang"                     "xmlLang"
   "xml:space"                    "xmlSpace"})

(defn map-svg-attributes
  [attrs]
  (reduce (fn [attrs key]
            (let [react-key (get svg-attributes-map (name key))]
              (if react-key
                (replace-key attrs key react-key)
                attrs)))
          attrs
          (keys attrs)))

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

(defn map-attribute-events
  [attrs {on-dom-event :on-dom-event}]
  (let [handle-dom-event (fn [{attributes-key :attributes-key
                               type           :type
                               event          :event}]
                           (on-dom-event {:type      type
                                          :dom-event :event
                                          :event     event
                                          :handlers  (formalize-event-handlers (get attrs attributes-key))}))]
    (-> attrs
        (change-attribute {:key :on-click :new-key :onClick :assoc (fn [e] (handle-dom-event {:attributes-key :on-click :type :on-click :event e}))})
        (change-attribute {:key :on-mouse-enter :new-key :onMouseEnter :assoc (fn [e] (handle-dom-event {:attributes-key :on-mouse-enter :type :on-mouse-enter :event e}))})
        (change-attribute {:key :on-mouse-leave :new-key :onMouseLeave :assoc (fn [e] (handle-dom-event {:attributes-key :on-mouse-leave :type :on-mouse-leave :event e}))})
        (change-attribute {:key :on-mouse-up :new-key :onMouseUp :assoc (fn [e] (handle-dom-event {:attributes-key :on-mouse-up :type :on-mouse-up :event e}))})
        (change-attribute {:key :on-mouse-down :new-key :onMouseDown :assoc (fn [e] (handle-dom-event {:attributes-key :on-mouse-down :type :on-mouse-down :event e}))})
        (change-attribute {:key :on-change :new-key :onChange :assoc (fn [e] (handle-dom-event {:attributes-key :on-change :type :on-change :event e}))}))))

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
                 {:style {"paddingLeft" ""}})
            ;; Map special SVG attributes
            (is= (map-to-react-attributes {:text-anchor "foo" :xlink:href "bar"} {})
                 {"textAnchor" "foo" "xlinkHref" "bar"}))}
  map-to-react-attributes [attrs {on-dom-event :on-dom-event :as args}]
  (-> attrs
      (change-attribute {:key :class :new-key :className})
      (map-attribute-events args)
      (change-attribute {:key :style :update style->react-style})
      (map-svg-attributes)))


(defn add-key-attribute
  {:test (fn []
           ;; Should add key if present in system options, and not in element attrs.
           (is= (add-key-attribute [:div {}] "key-value")
                [:div {:key "key-value"}])
           ;; Should not override existing key attribute.
           (is= (map-to-react-attributes {:key "b"} {:key "a"})
                {:key "b"}))}
  [vdom-element key-value]
  (update vdom-element 1 (fn [attrs]
                           (if (contains? attrs :key)
                             attrs
                             (assoc attrs :key key-value)))))
