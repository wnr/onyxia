(ns example-app.app
  (:require [example-app.module.view.hearthstone.cards.id1.cards-table :as cards-table]))

(defn get-cards-page-view-definition []
  {:name "cards-list-page-view"
   :dependencies [(cards-table/get-view-definition)]
   :render (fn []
             [:div
              [:h1 "Hearthstone Cards"]
              [:view {:name (:name (cards-table/get-view-definition))}]])})

(defn get-app-view-definition []
  {:name         "app-view"
   :dependencies [(get-cards-page-view-definition)]
   :render       (fn []
                   [:div {:class "app"}
                    [:header
                     [:nav {:class "navbar"}
                      [:h1 "Cards"]]]
                    [:div {:class "body"}
                     [:view {:name (:name (get-cards-page-view-definition))}]]
                    [:div {:class "footer"}]])})
