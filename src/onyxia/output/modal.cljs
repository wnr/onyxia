(ns onyxia.output.modal)

(defonce system-atom (atom {:modal-element nil
                            :current-modal-view nil
                            :wanted-modal-view nil
                            :render nil}))

(defn get-modal-element! []
  (:modal-element @system-atom))

(defn create-modal-element! []
  (let [modal-element (.createElement js/document "div")]
    (aset modal-element "className" "modal")
    (.appendChild (.-body js/document) modal-element)
    (swap! system-atom assoc :modal-element modal-element)))

(defn set-wanted-modal-view [state view]
  (assoc state :wanted-modal-view view))

(defn get-wanted-modal-view [state]
  (:wanted-modal-view state))

(defn clear-wanted-modal-view [state]
  (assoc state :wanted-modal-view nil))

(defn get-current-modal-view [state]
  (:current-modal-view state))

(defn set-current-modal-view [state view]
  (assoc state :current-modal-view view))

(defn render! [view]
  ((:render @system-atom) {:view view
                           :target-element (:modal-element @system-atom)
                           ;; TODO: How to handle input/output definitions?
                           }))

(defn modal-component [{title :title
                        body  :body}]
  [:div {:style {"position"        "absolute"
                 "top"             "0px"
                 "left"            "0px"
                 "width"           "100%"
                 "height"          "100%"
                 "display"         "flex"
                 "align-items"     "center"
                 "justify-content" "center"}}
   [:div {:style {"position"   "absolute"
                  "top"        "0px"
                  "left"       "0px"
                  "width"      "100%"
                  "height"     "100%"
                  "background" "black"
                  "opacity"    "0.7"}}]
   [:div {:style {"position"   "absolute"
                  "background" "white"
                  "opacity"    "1"
                  "left"       "auto"
                  "right"      "auto"}}
    [:h1 title]
    [:div body]]])

(defn handle-current-modal-view! []
  (let [state        @system-atom
        wanted-view  (get-wanted-modal-view state)
        current-view (get-current-modal-view state)]
    (when (not= wanted-view current-view)
      (if wanted-view
        (render! (modal-component wanted-view))
        (when current-view
          (render! nil)))
      (swap! system-atom set-current-modal-view wanted-view))))

(add-watch system-atom
           :modal-handler
           (fn [key atom old-state new-state]
             (handle-current-modal-view!)))


(if (nil? (get-modal-element!))
  (create-modal-element!)
  (swap! system-atom set-current-modal-view nil))

(defn get-definition [{render :render}]
  (swap! system-atom assoc :render render) ;; TODO: Not nice.
  {:name         "modal"
   :handle! (fn [output state]
              (let [modal-view ((:get-modal output) state)]
                (if modal-view
                  (swap! system-atom set-wanted-modal-view modal-view)
                  (swap! system-atom clear-wanted-modal-view))))})
