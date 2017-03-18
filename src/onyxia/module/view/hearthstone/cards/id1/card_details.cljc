(ns onyxia.module.view.hearthstone.cards.id1.card-details
  (:require
   [ysera.test :refer [is= is]]))

(defn create-state []
  {:read-more false})

(defn reading-more-about-card? [state]
  (:read-more state))

(defn toggle-read-more-about-card [state card]
  (assoc state :read-more (not (reading-more-about-card? state))))

(defn get-view-definition []
  {:name              "view.hearthstone.cards.id1/card-details"
   :input             {:size {:name      "parent-size"
                              :dimension :width}}
   :get-initial-state create-state
   :output            [{:name      "modal"
                        :get-modal (fn [{state :view-state
                                         card  :card}]
                                     (println "get-modal" state)
                                     (when (reading-more-about-card? state)
                                       {:title (str "Om " (:category card))
                                        :body  [:p "Bla bla bla..."]}))}]
   :render            (fn [{state :view-state
                            size  :size
                            card  :card}]
                        [:div
                         [:div (str "Reading more about card: " (reading-more-about-card? state))]
                         [:p
                          (map (fn [[key value]]
                                 [:span (str (name key) ": " value)
                                  [:br]]) card)]
                         [:button {:on-click [:on-read-more-click {:card card}]} "Läs mer"]])
   :events            {:on-read-more-click (fn [view-state {card :card}]
                                             (toggle-read-more-about-card view-state card))}})
