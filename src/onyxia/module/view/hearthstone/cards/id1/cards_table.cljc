(ns onyxia.module.view.hearthstone.cards.id1.cards-table
  (:require
   [ysera.test :refer [is= is]]
   [onyxia.module.view.hearthstone.cards.id1.card-details :as card-details]
   [onyxia.module.view.hearthstone.cards.id1.mocks :as mocks]))

(defn- create-state []
  {:expanded-card-detail-ids #{}})

(defn-
  ^{:test (fn []
            (is (card-detail-expanded? {:expanded-card-detail-ids #{"foo"}} "foo"))
            (is (not (card-detail-expanded? {:expanded-card-detail-ids #{"foo"}} "bar"))))}
  card-detail-expanded? [state id]
  (contains? (:expanded-card-detail-ids state) id))

(defn-
  ^{:test (fn []
            (is= (-> (create-state)
                     (expand-card-detail "foo")
                     (:expanded-card-detail-ids))
                 #{"foo"}))}
  expand-card-detail [state id]
  (if (card-detail-expanded? state id)
      state
      (update state :expanded-card-detail-ids conj id)))

(defn-
  ^{:test (fn []
            (is= (-> (create-state)
                     (expand-card-detail "foo")
                     (collapse-card-detail "foo")
                     (:expanded-card-detail-ids))
                 #{}))}
  collapse-card-detail [state id]
  (if (not (card-detail-expanded? state id))
      state
      (update state :expanded-card-detail-ids disj id)))

(defn-
  ^{:test (fn []
            (is= (-> (create-state)
                     (toggle-expand-card-detail "foo")
                     (:expanded-card-detail-ids))
                 #{"foo"})
            (is= (-> (create-state)
                     (expand-card-detail "foo")
                     (toggle-expand-card-detail "foo")
                     (:expanded-card-detail-ids))
                 #{}))}
  toggle-expand-card-detail [state id]
  (if (card-detail-expanded? state id)
    (collapse-card-detail state id)
    (expand-card-detail state id)))

(defn get-view-definition []
  {:name              "view.hearthstone.cards.id1/cards-table"
   :dependencies      [(card-details/get-view-definition)]
   :input             {:size {:name      "parent-size"
                              :dimension :width}}
   :get-initial-state create-state
   :events            {:on-row-click (fn [view-state {card-id :card-id}]
                                       (toggle-expand-card-detail view-state card-id))}
   :render            (fn [{size       :size
                            view-state :view-state}]
                        [:div
                         [:h1 (str size)]
                         [:table {:class "table"}
                          [:thead
                           [:tr
                            [:th "Name"]
                            [:th "Race"]
                            [:th "Set"]]]
                          [:tbody
                           (map (fn [card]
                                  [[:tr {:role "button" :on-click [:on-row-click {:card-id (:id card)}]}
                                    [:td (:name card)]
                                    [:td (:race card)]
                                    [:td (:set card)]]
                                   (when (card-detail-expanded? view-state (:id card))
                                     [:tr
                                      [:td {:colSpan 3}
                                       [:view {:name  (:name (card-details/get-view-definition))
                                               :input {:card card}}]]])]) ;; data
                                (:cards mocks/get-cards))]]])})
