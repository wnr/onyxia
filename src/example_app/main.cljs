(ns example-app.main
  (:require [ysera.error :refer [error]]
            [onyxia.react :refer [render!]]
            [onyxia.input.parent-size]
            [onyxia.output.modal]
            [example-app.app :refer [app-view-definition]]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(defn render-app! []
  (render!
   {:view [:view {:definition app-view-definition}]
    :target-element (js/document.getElementById "app")
    :input-definitions {"parent-size" (onyxia.input.parent-size/get-definition)}
    :output-definitions {"modal" (onyxia.output.modal/get-definition {:render render!})}}))

(render-app!)
