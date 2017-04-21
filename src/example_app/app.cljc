(ns example-app.app
  (:require [example-app.module.view.hearthstone.cards.id1.cards-table :as cards-table]))

(def cards-page-view-definition
  {:name "cards-list-page-view"
   :render (fn []
             [:div
              [:h1 "Hearthstone Cards"]
              [:view {:definition cards-table/view-definition}]])})

(def app-view-definition
  {:name         "app-view"
   :render       (fn []
                   [:div {:class "app"}
                    [:header
                     [:nav {:class "navbar"}
                      [:h1 "Cards"]]]
                    [:div {:class "body"}
                     [:view {:definition cards-page-view-definition}]]
                    [:div {:class "footer"}]])})
