(ns onyxia.attributes-map)

(def attribute-map {:background-color :backgroundColor
                    :border-bottom :borderBottom
                    :border-collapse :borderCollapse
                    :border-radius :borderRadius
                    :border-top :borderTop
                    :font-size :fontSize
                    :line-height :lineHeight
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
                    :text-align :textAlign
                    :transform-origin :transformOrigin
                    :transform-style :transformStyle
                    :vertical-align :verticalAlign})

(defn kebab->camel [key]
  (if-let [camel-key (get attribute-map key)]
    camel-key
    key))
