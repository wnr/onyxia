(ns onyxia.attributes-map
  (:require [clojure.string :as string]))

;; Many attributes differ from the html DOM key and JavaScript prop key (kebab case vs camel case).
;; Many rendering frameworks use the camel case style, but the onyxia api uses the kebab case.
;; We want calculate kebab -> camel at runtime to support future attributes. For performance, we
;; cache the calculations so that each mapping is only done once.
;; Also, we prep the cache atom with known mappings at development time.
(def style-kebab-to-camel-map-atom (atom {:-moz-font-smoothing    :MozFontSmoothing
                                          :-webkit-appearance     :WebkitAppearance
                                          :-webkit-font-smoothing :WebkitFontSmoothing
                                          :align-items            :alignItems
                                          :background-color       :backgroundColor
                                          :border-bottom          :borderBottom
                                          :border-collapse        :borderCollapse
                                          :border-radius          :borderRadius
                                          :border-top             :borderTop
                                          :box-shadow             :boxShadow
                                          :clip-path              :clipPath
                                          :font-family            :fontFamily
                                          :font-size              :fontSize
                                          :font-smoothing         :fontSmoothing
                                          :font-weight            :fontWeight
                                          :justify-content        :justifyContent
                                          :line-height            :lineHeight
                                          :list-style             :listStyle
                                          :margin-bottom          :marginBottom
                                          :margin-left            :marginLeft
                                          :margin-right           :marginRight
                                          :margin-top             :marginTop
                                          :max-height             :maxHeight
                                          :max-width              :maxWidth
                                          :min-width              :minWidth
                                          :on-change              :onChange
                                          :on-click               :onClick
                                          :on-input               :onInput
                                          :on-mouse-down          :onMouseDown
                                          :on-mouse-enter         :onMouseEnter
                                          :on-mouse-leave         :onMouseLeave
                                          :on-mouse-up            :onMouseUp
                                          :on-key-down            :onKeyDown
                                          :overflow-y             :overflowY
                                          :padding-bottom         :paddingBottom
                                          :padding-left           :paddingLeft
                                          :padding-right          :paddingRight
                                          :padding-top            :paddingTop
                                          :pointer-events         :pointerEvents
                                          :text-align             :textAlign
                                          :text-anchor            :textAnchor
                                          :text-decoration        :textDecoration
                                          :text-shadow            :textShadow
                                          :text-rendering         :textRendering
                                          :transform-origin       :transformOrigin
                                          :transform-style        :transformStyle
                                          :user-select            :userSelect
                                          :vertical-align         :verticalAlign
                                          :white-space            :whiteSpace
                                          :word-break             :wordBreak
                                          :z-index                :zIndex}))

;; Many of these functions are borrowed from reagent https://github.com/reagent-project/reagent/blob/580729178bdba98edb68e1109f07c3a13ad93780/src/reagent/impl/util.cljs
(defn capitalize [s]
  (if (< (count s) 2)
    (string/upper-case s)
    (str (string/upper-case (subs s 0 1)) (subs s 1))))

(defn- _kebab->camel [dashed]
  (if (string? dashed)
    dashed
    (let [name-str (name dashed)
          [start & parts] (string/split name-str #"-")]
      (apply str start (map capitalize parts)))))

(defn kebab->camel [kebab]
  (let [cached-camel (get (deref style-kebab-to-camel-map-atom) kebab)]
    (if cached-camel
      cached-camel
      (let [camel (_kebab->camel kebab)]
        (swap! style-kebab-to-camel-map-atom assoc kebab camel)
        camel))))
