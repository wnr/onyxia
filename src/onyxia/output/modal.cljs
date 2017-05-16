(ns onyxia.output.modal)

(def system-atom (atom {:modal-element      nil
                        :current-modal-view nil
                        :wanted-modal-view  nil}))

(defn get-modal-element! []
  (:modal-element @system-atom))

(defn create-modal-element! []
  (let [modal-element (.createElement js/document "div")]
    (aset modal-element "className" "modal")
    (.appendChild (.-body js/document) modal-element)
    (swap! system-atom assoc :modal-element modal-element)))

(defn set-wanted-modal-view-context [state view-context]
  (assoc state :wanted-modal-view-context view-context))

(defn get-wanted-modal-view-context [state]
  (:wanted-modal-view-context state))

(defn clear-wanted-modal-view-context [state]
  (assoc state :wanted-modal-view-context nil))

(defn get-current-modal-view-context [state]
  (:current-modal-view-context state))

(defn set-current-modal-view-context [state view-context]
  (assoc state :current-modal-view-context view-context))

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

(defn handle-current-modal-view! []
  (let [state @system-atom
        wanted-view-context (get-wanted-modal-view-context state)
        current-view-context (get-current-modal-view-context state)]
    (when (not= wanted-view-context current-view-context)
      (if wanted-view-context
        ((:render! wanted-view-context)
          {:view                (modal-component (:view wanted-view-context))
           :target-element      (:modal-element @system-atom)
           :input-definitions   (:input-definitions wanted-view-context)
           :output-definitions  (:output-definitions wanted-view-context)
           :ancestor-views-data (:ancestor-views-data wanted-view-context)})
        (when current-view-context
          ((:render! current-view-context)
            {:target-element (:modal-element @system-atom)
             :view           nil})))
      (swap! system-atom set-current-modal-view-context wanted-view-context))))

(add-watch system-atom
           :modal-handler
           (fn [key atom old-state new-state]
             (handle-current-modal-view!)))


(if (nil? (get-modal-element!))
  (create-modal-element!)
  (swap! system-atom set-current-modal-view-context nil))

(defn get-definition []
  {:name    "modal"
   :handle! (fn [{view-output         :view-output
                  view-state          :view-state
                  render!             :render!
                  input-definitions   :input-definitions
                  output-definitions  :output-definitions
                  ancestor-views-data :ancestor-views-data}]
              (let [modal-view ((:get-modal view-output) view-state)]
                (if modal-view
                  (swap! system-atom set-wanted-modal-view-context {:view                modal-view
                                                                    :render!             render!
                                                                    :input-definitions   input-definitions
                                                                    :output-definitions  output-definitions
                                                                    :ancestor-views-data ancestor-views-data})
                  (swap! system-atom clear-wanted-modal-view-context))))})
