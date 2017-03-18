(ns onyxia.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ysera.error :refer [error]]
            [onyxia.dom-operator :as dom-operator]
            [onyxia.view-definitions :as view-definitions]
            [onyxia.input-definitions :as input-definitions]
            [onyxia.output-definitions :as output-definitions]
            [onyxia.input.parent-size]
            [onyxia.output.modal]
            [onyxia.app :refer [get-app-view-definition]]))

;;; Open Problems:
;; * How to handle the uniqueness of the component definition names?

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(input-definitions/add! (onyxia.input.parent-size/get-definition))

(output-definitions/add! (onyxia.output.modal/get-definition {:render dom-operator/render!}))

(view-definitions/add-with-dependencies! (get-app-view-definition))

(defn render-app! []
  (dom-operator/render!
   [:view {:name (:name (get-app-view-definition))}]
   (js/document.getElementById "app")))

(render-app!)
