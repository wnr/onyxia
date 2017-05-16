(ns example-app.module.view.hearthstone.cards.id1.button)

(def button-view-alternative
  {:name              "button"
   :get-initial-state (fn []
                        {:hovered false
                         :active  false})
   :events            {:on-mouse-enter (fn [view-state _]
                                         (-> view-state
                                             (assoc :hovered true)
                                             (assoc :active false)))
                       :on-mouse-down  (fn [view-state _]
                                         (-> view-state
                                             (assoc :hovered true)
                                             (assoc :active true)))
                       :on-mouse-leave (fn [view-state _]
                                         (-> view-state
                                             (assoc :hovered false)
                                             (assoc :active false)))
                       :on-mouse-up    (fn [view-state _]
                                         (-> view-state
                                             (assoc :hovered true)
                                             (assoc :active false)))
                       :on-click       (fn [view-state _]
                                         (println "click"))}
   :render            (fn [{{hovered :hovered active :active} :view-state}]
                        [:div {:style          (merge {:display    "inline-block"
                                                       :background "rebeccapurple"
                                                       :color      "#fff"
                                                       :border     "none"
                                                       :padding    "1rem 2rem 1rem 2rem"
                                                       :fontWeight "bold"
                                                       :fontSize   "1rem"
                                                       :cursor     "pointer"
                                                       :outline    "none"}
                                                      (when hovered
                                                        {:filter "brightness(120%)"})
                                                      (when active
                                                        {:filter "brightness(85%)"}))
                               :on-mouse-down  [:on-mouse-down nil]
                               :on-mouse-enter [:on-mouse-enter nil]
                               :on-mouse-up    [:on-mouse-up nil]
                               :on-mouse-leave [:on-mouse-leave nil]
                               :on-click       [:on-click nil]}
                         "click me"])})

(def button-view
  {:name   "button"
   :input  {:hovered {:name "element-hovered"}
            :active  {:name "element-active"}}
   :render (fn [{hovered  :hovered
                 active   :active
                 children :children
                 on-click :on-click}]
             [:div {:style                 (merge {:display    "inline-block"
                                                   :background "rebeccapurple"
                                                   :color      "#fff"
                                                   :padding    "1rem 2rem 1rem 2rem"
                                                   :fontWeight "bold"
                                                   :fontSize   "1rem"
                                                   :cursor     "pointer"
                                                   :outline    "none"}
                                                  (when hovered
                                                    {:filter "brightness(120%)"})
                                                  (when active
                                                    {:filter "brightness(85%)"}))
                    :element-hovered-value true
                    :element-active-value  true
                    :on-click              on-click}
              children])})