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

; Taken from https://github.com/facebook/react/blob/b1b4a2fb252f26fe10d29ba60d85ff89a85ff3ec/src/renderers/dom/shared/SVGDOMPropertyConfig.js
(def svg-attributes-map
  {"accent-height" "accentHeight"
   "alignment-baseline" "alignmentBaseline"
   "arabic-form" "arabicForm"
   "baseline-shift" "baselineShift"
   "cap-height" "capHeight"
   "clip-path" "clipPath"
   "clip-rule" "clipRule"
   "color-interpolation" "colorInterpolation"
   "color-interpolation-filters" "colorInterpolationFilters"
   "color-profile" "colorProfile"
   "color-rendering" "colorRendering"
   "dominant-baseline" "dominantBaseline"
   "enable-background" "enableBackground"
   "fill-opacity" "fillOpacity"
   "fill-rule" "fillRule"
   "flood-color" "floodColor"
   "flood-opacity" "floodOpacity"
   "font-family" "fontFamily"
   "font-size" "fontSize"
   "font-size-adjust" "fontSizeAdjust"
   "font-stretch" "fontStretch"
   "font-style" "fontStyle"
   "font-variant" "fontVariant"
   "font-weight" "fontWeight"
   "glyph-name" "glyphName"
   "glyph-orientation-horizontal" "glyphOrientationHorizontal"
   "glyph-orientation-vertical" "glyphOrientationVertical"
   "horiz-adv-x" "horizAdvX"
   "horiz-origin-x" "horizOriginX"
   "image-rendering" "imageRendering"
   "letter-spacing" "letterSpacing"
   "lighting-color" "lightingColor"
   "marker-end" "markerEnd"
   "marker-mid" "markerMid"
   "overline-position" "overlinePosition"
   "overline-thickness" "overlineThickness"
   "paint-order" "paintOrder"
   "panose-1" "panose1"
   "pointer-events" "pointerEvents"
   "rendering-intent" "renderingIntent"
   "shape-rendering" "shapeRendering"
   "stop-color" "stopColor"
   "stop-opacity" "stopOpacity"
   "strikethrough-position" "strikethroughPosition"
   "strikethrough-thickness" "strikethroughThickness"
   "stroke-dasharray" "strokeDasharray"
   "stroke-dashoffset" "strokeDashoffset"
   "stroke-linecap" "strokeLinecap"
   "stroke-linejoin" "strokeLinejoin"
   "stroke-miterlimit" "strokeMiterlimit"
   "stroke-opacity" "strokeOpacity"
   "stroke-width" "strokeWidth"
   "text-anchor" "textAnchor"
   "text-decoration" "textDecoration"
   "text-rendering" "textRendering"
   "underline-position" "underlinePosition"
   "underline-thickness" "underlineThickness"
   "unicode-bidi" "unicodeBidi"
   "unicode-range" "unicodeRange"
   "units-per-em" "unitsPerEm"
   "v-alphabetic" "vAlphabetic"
   "v-hanging" "vHanging"
   "v-ideographic" "vIdeographic"
   "v-mathematical" "vMathematical"
   "vector-effect" "vectorEffect"
   "vert-adv-y" "vertAdvY"
   "vert-origin-x" "vertOriginX"
   "vert-origin-y" "vertOriginY"
   "word-spacing" "wordSpacing"
   "writing-mode" "writingMode"
   "x-height" "xHeight"
   "xlink:actuate" "xlinkActuate"
   "xlink:arcrole" "xlinkArcrole"
   "xlink:href" "xlinkHref"
   "xlink:role" "xlinkRole"
   "xlink:show" "xlinkShow"
   "xlink:title" "xlinkTitle"
   "xlink:type" "xlinkType"
   "xml:base" "xmlBase"
   "xmlns:xlink" "xmlnsXlink"
   "xml:lang" "xmlLang"
   "xml:space" "xmlSpace"})

(defn- replace-key
  ([map key new-key]
   (replace-key map key new-key (get map key)))
  ([map key new-key value]
   (if (contains? map key)
     (-> (dissoc map key)
         (assoc new-key value))
     map)))

(defn- replace-value
  [map key value]
  (if (contains? map key)
    (update map key value)
    map))

(defn map-svg-attributes
  [attrs]
  (reduce (fn [attrs key]
            (let [react-key (get svg-attributes-map (name key))]
              (if react-key
                (replace-key attrs key react-key)
                attrs)))
          attrs
          (keys attrs)))

(defn
  ^{:test (fn []
            ;; Keep unknown attributes unchanged.
            (is= (map-to-react-attributes :div {:unknown "test"} {})
                 {:unknown "test"})
            ;; :class -> :className
            (is= (map-to-react-attributes :div {:class "foo bar"} {})
                 {:className "foo bar"})
            ;; :style -> :style (with values changed to react-flavor).
            (is= (map-to-react-attributes :div {:style {"padding-left" ""}} {})
                 {:style {"paddingLeft" ""}})
            ;; Map special SVG attributes
            (is= (map-to-react-attributes :div {:text-anchor "foo" :xlink:href "bar"} {})
                 {"textAnchor" "foo" "xlinkHref" "bar"}))}
  map-to-react-attributes [element attrs {on-dom-event :on-dom-event}]
  (-> attrs
      (replace-key :class :className)
      (replace-value :style style->react-style)
      (replace-key :on-click :onClick (fn [event] (on-dom-event {:type :on-click
                                                                 :dom-event :event
                                                                 :data (:on-click attrs)})))
      (map-svg-attributes)))
