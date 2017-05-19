(ns example-app.main
  (:require [ysera.error :refer [error]]
            [onyxia.engine.inferno :refer [render!]]
            [onyxia.input.parent-size]
            [onyxia.input.element-hovered]
            [onyxia.input.element-active]
            [onyxia.input.mouse-position]
            [onyxia.output.modal]
            [onyxia.output.at-body-root-view]
            [example-app.app :refer [app-view-definition]]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(defn render-app! []
  (render!
    {:view               app-view-definition
     :target-element     (js/document.getElementById "app")
     :input-definitions  {"parent-size"     (onyxia.input.parent-size/get-definition)
                          "element-hovered" (onyxia.input.element-hovered/get-definition
                                              {:mouse-position-input-definition (onyxia.input.mouse-position/get-definition)})
                          "element-active"  (onyxia.input.element-active/get-definition)}
     :output-definitions {"modal" (onyxia.output.modal/get-definition
                                    {:at-body-root-view-output-definition (onyxia.output.at-body-root-view/get-definition)})}}))

(defn on-js-reload
  []
  ;(render! {:view           nil
  ;          :target-element (js/document.getElementById "app")})
  ;(render-app!)
  )

(render-app!)
