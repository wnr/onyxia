(ns onyxia.attributes-map)

(def attribute-map {:-moz-font-smoothing    :MozFontSmoothing
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
                    :z-index                :zIndex})

(defn kebab->camel [key]
  (if-let [camel-key (get attribute-map key)]
    camel-key
    key))
