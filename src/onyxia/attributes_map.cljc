(ns onyxia.attributes-map)

(def attribute-map {:align-items :alignItems
                    :background-color :backgroundColor
                    :border-bottom :borderBottom
                    :border-collapse :borderCollapse
                    :border-radius :borderRadius
                    :border-top :borderTop
                    :box-shadow :boxShadow
                    :clip-path :clipPath
                    :font-family :fontFamily
                    :font-size :fontSize
                    :justify-content :justifyContent
                    :line-height :lineHeight
                    :list-style :listStyle
                    :margin-bottom :marginBottom
                    :margin-left :marginLeft
                    :margin-right :marginRight
                    :margin-top :marginTop
                    :max-height :maxHeight
                    :max-width :maxWidth
                    :min-width :minWidth
                    :on-change :onChange
                    :on-click :onClick
                    :on-input :onInput
                    :on-mouse-down :onMouseDown
                    :on-mouse-enter :onMouseEnter
                    :on-mouse-leave :onMouseLeave
                    :on-mouse-up :onMouseUp
                    :overflow-y :overflowY
                    :padding-bottom :paddingBottom
                    :padding-left :paddingLeft
                    :padding-right :paddingRight
                    :padding-top :paddingTop
                    :pointer-events :pointerEvents
                    :text-align :textAlign
                    :text-anchor :textAnchor
                    :text-shadow :textShadow
                    :transform-origin :transformOrigin
                    :transform-style :transformStyle
                    :user-select :userSelect
                    :vertical-align :verticalAlign
                    :z-index :zIndex})

(defn kebab->camel [key]
  (if-let [camel-key (get attribute-map key)]
    camel-key
    key))
