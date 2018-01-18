(ns example-app.app
  (:require [example-app.module.view.hearthstone.cards.id1.cards-table :as cards-table]
            [example-app.module.view.hearthstone.cards.id1.button :as button]
            [example-app.animation.sin-wave :as sin-wave]))

(def cards-page-view-definition
  {:name   "cards-list-page-view"
   :render (fn []
             [:div
              [:h1 "Hearthstone Cards"]
              [cards-table/view-definition]])})

(def app-view-definition
  {:name              "app-view"
   :get-initial-state (fn []
                        {:foo false})
   :events            {:on-click-1 (fn [view-state _]
                                     {:foo true})
                       :on-click-2 (fn [view-state _]
                                     {:foo false})}
   :render            (fn [{view-state :view-state}]
                        [:div {:class "app"}
                         [:header
                          [:nav {:class "navbar"}
                           [:h1 "Cards"]]]
                         [:div
                          [:div {:style {:margin "20px"}}
                           [:span {:style {:margin-right "20px"}}
                            [button/button-view {:on-click [app-view-definition :on-click-1 nil]}
                             "true"]]
                           [button/button-view {:on-click [app-view-definition :on-click-2 nil]}
                            "false"]
                           [:div {:style {:margin-top "20px"}}
                            (str (:foo view-state))]]]

                         [:div {:class "body"}
                          [cards-page-view-definition]]
                         [:div {:class "footer"}]
                         [:div {:style {:margin-top "20px"}}
                          [sin-wave/view]]
                         ])})
