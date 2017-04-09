(ns example-app.main
  (:require [ysera.error :refer [error]]
            [onyxia.react :refer [render!]]
            [onyxia.view-definitions :as view-definitions]
            [onyxia.input-definitions :as input-definitions]
            [onyxia.output-definitions :as output-definitions]
            [onyxia.input.parent-size]
            [onyxia.output.modal]
            [example-app.app :refer [get-app-view-definition]]))

;;; Open Problems:
;; * How to handle the uniqueness of the component definition names?

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(input-definitions/add! (onyxia.input.parent-size/get-definition))

(output-definitions/add! (onyxia.output.modal/get-definition {:render render!}))

(view-definitions/add-with-dependencies! (get-app-view-definition))

(defn render-app! []
  (render!
   [:view {:name (:name (get-app-view-definition))}]
   (js/document.getElementById "app")))

(render-app!)
