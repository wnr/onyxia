(ns example-app.module.view.hearthstone.cards.id1.cards-table
  (:require
    [ysera.test :refer [is= is]]
    [example-app.module.view.hearthstone.cards.id1.card-details :as card-details]
    [example-app.module.view.hearthstone.cards.id1.mocks :as mocks]))

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

(def table-style {:width          "100%"
                  :text-align     "left"
                  :border         "2px solid grey"
                  :border-spacing "0"})

(def th-style {:padding       "0.5rem"
               :border-bottom "1px solid black"})

(def table
  {:name   "view.hearthstone.cards.id1/table"
   :input  [{:name "element-hovered"
             :keys [{:input  :hovered-id
                     :update :element-hovered-value}]}
            {:name      "element-active"
             :input-key :active-id}]
   :render (fn [{thead      :thead
                 tbody      :tbody
                 hovered-id :hovered-id
                 active-id  :active-id}]
             [:table {:style table-style}
              [:thead
               [:tr
                thead]]
              [:tbody
               (map-indexed (fn [index {expandable         :expandable
                                        expanded           :expanded
                                        expandable-content :expandable-content
                                        columns            :columns
                                        on-click           :on-click}]
                              (if expandable
                                [[:tr {:role                  "button"
                                       :key                   (str "row-" index) ;; TODO Would be nice to avoid this.
                                       :style                 (merge {:cursor              "pointer"
                                                                      :transition-property "background-color"
                                                                      :transition-duration "70ms"}
                                                                     (when (and (not= index 0) (= index hovered-id))
                                                                       {:background "#eee"})
                                                                     (when (and (not= index 0) (= index active-id))
                                                                       {:background "#ddd"}))
                                       :on-click              on-click
                                       :element-hovered-value index
                                       :element-active-value  index}
                                  columns]
                                 (when expanded
                                   [:tr {:key (str "expandable-" index)}
                                    expandable-content])]
                                [:tr columns]))
                            tbody)]])})

(def view-definition
  {:name              "view.hearthstone.cards.id1/cards-table"
   :input             [{:name      "parent-size"
                        :dimension :width
                        :input-key :size}]
   :get-initial-state create-state
   :events            {:on-row-click (fn [view-state {card-id :card-id}]
                                       (toggle-expand-card-detail view-state card-id))}
   :render            (fn [{size       :size
                            view-state :view-state}]
                        [:div
                         [:h1 (str (:width size))]
                         [table
                          {:thead [[:th {:style th-style} "Name"]
                                   [:th {:style th-style} "Race"]
                                   [:th {:style th-style} "Set"]]
                           :tbody (map (fn [card]
                                         {:on-click           [view-definition :on-row-click {:card-id (:id card)}]
                                          :expandable         true
                                          :expanded           (card-detail-expanded? view-state (:id card))
                                          :columns            [[:td (:name card)]
                                                               [:td (:race card)]
                                                               [:td (:set card)]]
                                          :expandable-content [:td {:colSpan 3}
                                                               [card-details/view-definition {:card card}]]})
                                       (:cards mocks/get-cards))}]])})
