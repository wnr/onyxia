(ns example-app.module.view.hearthstone.cards.id1.card-details
  (:require
    [ysera.test :refer [is= is]]
    [example-app.module.view.hearthstone.cards.id1.button :refer [button-view]]))

(defn create-state []
  {:read-more false})

(defn reading-more-about-card? [state]
  (:read-more state))

(defn toggle-read-more-about-card [state card]
  (assoc state :read-more (not (reading-more-about-card? state))))

(defn stop-reading-more-about-card [state]
  (assoc state :read-more false))

(def view-definition
  {:name              "view.hearthstone.cards.id1/card-details"
   :input             [{:name      "parent-size"
                        :dimension :width
                        :input-key :size}]
   :get-initial-state create-state
   :output            [{:name      "modal"
                        :get-modal (fn [{state :view-state
                                         card  :card}]
                                     (when (reading-more-about-card? state)
                                       {:title (str "About " (:category card))
                                        :body  [:div
                                                [:p "Bla bla bla..."]
                                                [button-view {:on-click [view-definition :on-close-modal-click]}
                                                 "Close"]]}))}]
   :render            (fn [{state :view-state
                            size  :size
                            card  :card}]
                        [:div
                         [:div (str "Reading more about card: " (reading-more-about-card? state))]
                         [:p
                          (map (fn [[key value]]
                                 [:span {:key key} (str (name key) ": " value)
                                  [:br]]) card)]
                         [:button {:on-click [:on-read-more-click {:card card}]} "Läs mer"]])
   :events            {:on-read-more-click   (fn [view-state {card :card}]
                                               (toggle-read-more-about-card view-state card))
                       :on-close-modal-click (fn [view-state _]
                                               (stop-reading-more-about-card view-state))}})
