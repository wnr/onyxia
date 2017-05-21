(ns onyxia.output.modal)

(defn modal-component [{title :title
                        body  :body}]
  [:div {:style {:position        "absolute"
                 :top             "0px"
                 :left            "0px"
                 :width           "100%"
                 :height          "100%"
                 :display         "flex"
                 :align-items     "center"
                 :justify-content "center"}}
   [:div {:style {:position   "absolute"
                  :top        "0px"
                  :left       "0px"
                  :width      "100%"
                  :height     "100%"
                  :background "black"
                  :opacity    "0.7"}}]
   [:div {:style {:position   "absolute"
                  :background "white"
                  :opacity    "1"
                  :left       "auto"
                  :right      "auto"
                  :width      "500px"
                  :padding    "0 1rem 1rem 1rem"}}
    [:h1 title]
    [:div body]]])

(def definition
  {:name    "modal"
   :handle! (fn [{at-body-root-view :at-body-root-view-output-definition :as args}]
              ((:handle! at-body-root-view)
                (update args :view-output (fn [view-output]
                                            (-> view-output
                                                (assoc :get-view (fn [view-state]
                                                                   (when-let [modal-view ((:get-modal view-output) view-state)]
                                                                     (modal-component modal-view))))
                                                (dissoc :get-modal))))))})
