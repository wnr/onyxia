(ns onyxia.input.parent-size
  (:require [onyxia.dom-operator :refer [add-pending-operation!]]
            [ysera.error :refer [error]]))

(def system-state-atom (atom {:scrollbar-size              nil
                              :scrollbar-size-measurer-div nil
                              :system-ready-listeners      []}))

(defn system-ready!? []
  (not (nil? (:scrollbar-size @system-state-atom))))

(defn add-system-ready-listener! [listener]
  (swap! system-state-atom update :system-ready-listeners conj listener))

(defn check-system-ready! []
  (when (system-ready!?)
    (reduce (fn [_ listener]
              (listener))
            nil
            (:system-ready-listeners @system-state-atom))
    (swap! system-state-atom assoc :system-ready-listeners [])))

(defn get-element-state! [element]
  (aget element "_erd"))

(defn update-element-state! [element updator & args]
  (aset element "_erd" (apply updator (get-element-state! element) args)))

(defn get-expand-element! [element]
  (:expand-element (get-element-state! element)))

(defn get-expand-child-element! [element]
  (:expand-child-element (get-element-state! element)))

(defn get-shrink-element! [element]
  (:shrink-element (get-element-state! element)))

(defn get-shrink-child-element! [element]
  (:shrink-child-element (get-element-state! element)))

(defn get-container-element [element]
  (:container (get-element-state! element)))

(defn get-width-offset [scrollbar-width]
  (* 2 (+ scrollbar-width 1)))

(defn get-height-offset [scrollbar-height]
  (* 2 (+ scrollbar-height 1)))

(defn get-desired-expand-child-width [element-width scrollbar-width]
  (+ element-width 10 (get-width-offset scrollbar-width)))

(defn get-desired-expand-child-height [element-height scrollbar-height]
  (+ element-height 10 (get-height-offset scrollbar-height)))

(defn get-desired-shrink-child-width [element-width scrollbar-width]
  (+ (* 2 element-width) scrollbar-width))

(defn get-desired-shrink-child-height [element-height scrollbar-height]
  (+ (* 2 element-height) scrollbar-height))

(defn update-child-sizes! [element element-width element-height]
  (let [scrollbar-size (:scrollbar-size @system-state-atom)
        scrollbar-width (:width scrollbar-size)
        scrollbar-height (:height scrollbar-size)
        expand-child (get-expand-child-element! element)]
    (aset expand-child "style" "width" (str (get-desired-expand-child-width element-width scrollbar-width) "px"))
    (aset expand-child "style" "height" (str (get-desired-expand-child-height element-height scrollbar-height) "px"))))

(defn position-scrollbars! [element element-width element-height]
  (let [scrollbar-size (:scrollbar-size @system-state-atom)
        scrollbar-width (:width scrollbar-size)
        scrollbar-height (:height scrollbar-size)
        expand (get-expand-element! element)
        shrink (get-shrink-element! element)]
    (aset expand "scrollLeft" (get-desired-expand-child-width element-width scrollbar-width))
    (aset expand "scrollTop" (get-desired-expand-child-height element-height scrollbar-height))
    (aset shrink "scrollLeft" (get-desired-shrink-child-width element-width scrollbar-width))
    (aset shrink "scrollTop" (get-desired-shrink-child-height element-width scrollbar-height))))

(defn handle-scroll! [element]
  (add-pending-operation!
    {:operation :read-dom
     :execute!  (fn read []
                  (let [current-width (.-offsetWidth element)
                        current-height (.-offsetHeight element)
                        element-state (get-element-state! element)
                        last-known-width (:last-known-width element-state)
                        last-known-height (:last-known-height element-state)]
                    (when (or (not= current-width last-known-width)
                              (not= current-height last-known-height))
                      (doseq [listener (:listeners (.-_erd element))]
                        (listener {:width  current-width
                                   :height current-height}))
                      (add-pending-operation!
                        {:operation :write-dom
                         :execute!  (fn update-elements []
                                      (update-child-sizes! element current-width current-height)
                                      (position-scrollbars! element current-width current-height)
                                      (update-element-state! element assoc :last-known-width current-width)
                                      (update-element-state! element assoc :last-known-height current-height))}))))}))

(defn install! [{element   :element
                 on-resize :on-resize}]
  (let [_install! (fn install []
                    (if (.-_erd element)
                      (do
                        (set! (.-_erd element) (update (.-_erd element) :listeners conj on-resize)))
                      (do
                        (set! (.-_erd element) {:listeners [on-resize]})
                        (add-pending-operation!
                          {:operation :read-dom
                           :execute!  (fn read-element-size []
                                        (let [style (js/getComputedStyle element)]
                                          (update-element-state! element assoc :start-size {:width  (.-offsetWidth element)
                                                                                            :height (.-offsetHeight element)})
                                          (add-pending-operation!
                                            {:operation :write-dom
                                             :execute!  (fn inject-elements []
                                                          (let [position (aget style "position")]
                                                            (when (or (nil? position) (empty? position) (= position "static"))
                                                              (aset element "style" "position" "relative")))
                                                          (let [element-state (get-element-state! element)
                                                                element-width (:width (:start-size element-state))
                                                                element-height (:height (:start-size element-state))
                                                                scrollbar-size (:scrollbar-size @system-state-atom)
                                                                scrollbar-width (:width scrollbar-size)
                                                                scrollbar-height (:height scrollbar-size)
                                                                shrink-child (js/document.createElement "div")
                                                                expand-child (js/document.createElement "div")
                                                                shrink (js/document.createElement "div")
                                                                expand (js/document.createElement "div")
                                                                container-1 (js/document.createElement "div")
                                                                container-2 (js/document.createElement "div")
                                                                container-3 (js/document.createElement "div")
                                                                on-expand-scroll (fn onExpandScroll [event] (handle-scroll! element))
                                                                on-shrink-scroll (fn onShrinkScroll [event] (handle-scroll! element))]
                                                            (aset shrink-child "style" "cssText" "position: absolute; width: 200%; height: 200%;")
                                                            (update-element-state! element assoc :shrink-child-element shrink-child)
                                                            (aset expand-child "style" "cssText" (str "position: absolute; left: 0; top: 0; width: " (get-desired-expand-child-width element-width scrollbar-width) "px; height: " (get-desired-expand-child-height element-height scrollbar-height) "px;"))
                                                            (update-element-state! element assoc :expand-child-element expand-child)
                                                            (aset shrink "style" "cssText" "position: absolute; flex: none; overflow: scroll; z-index: -1; visibility: hidden; width: 100%; height: 100%;")
                                                            (aset shrink "dataset" "erdId" "shrink")
                                                            (update-element-state! element assoc :on-shrink-scroll on-shrink-scroll)
                                                            (update-element-state! element assoc :shrink-element shrink)
                                                            (.addEventListener shrink "scroll" on-shrink-scroll)
                                                            (aset expand "style" "cssText" "position: absolute; flex: none; overflow: scroll; z-index: -1; visibility: hidden; width: 100%; height: 100%;")
                                                            (aset expand "dataset" "erdId" "expand")
                                                            (update-element-state! element assoc :on-expand-scroll on-expand-scroll)
                                                            (update-element-state! element assoc :expand-element expand)
                                                            (.addEventListener expand "scroll" on-expand-scroll)
                                                            (aset container-1 "className" "erd_scroll_detection_container")
                                                            (aset container-1 "style" "cssText" (str "position: absolute; flex: none; overflow: hidden; z-index: -1; visibility: hidden; left: " (- (+ 1 scrollbar-width)) "px; top: " (- (+ 1 scrollbar-height)) "px; bottom: " (- scrollbar-height) "px; right: " (- scrollbar-width) "px;"))
                                                            (aset container-2 "className" "erd_scroll_detection_container")
                                                            (aset container-2 "style" "cssText" "position: absolute; flex: none; overflow: hidden; z-index: -1; visibility: hidden; width: 100%; height: 100%; left: 0px; top: 0px;")
                                                            (aset container-2 "dir" "ltr")
                                                            (aset container-3 "className" "erd_scroll_detection_container")
                                                            (aset container-3 "style" "cssText" "visibility: hidden; display: inline; width: 0px; height: 0px; z-index: -1; overflow: hidden; margin: 0; padding: 0;")
                                                            (.appendChild expand expand-child)
                                                            (.appendChild shrink shrink-child)
                                                            (.appendChild container-1 expand)
                                                            (.appendChild container-1 shrink)
                                                            (.appendChild container-2 container-1)
                                                            (.appendChild container-3 container-2)
                                                            (.appendChild element container-3)
                                                            (update-element-state! element assoc :container container-3)

                                                            ;; Can this be here?
                                                            (position-scrollbars! element element-width element-height)
                                                            (update-element-state! element assoc :last-known-width element-width)
                                                            (update-element-state! element assoc :last-known-height element-height)))})))}))))]
    (if (not (system-ready!?))
      (add-system-ready-listener! _install!)
      (_install!))))

(defn handle-unmount!
  [{element      :element
    root-element :root-element
    on-resize    :on-resize}]
  (when (.-_erd element)
    (if (= root-element element)
      (let [shrink (get-shrink-element! element)
            expand (get-expand-element! element)
            container (get-container-element element)
            element-state (get-element-state! element)]
        (.removeEventListener shrink "scroll" (:on-shrink-scroll element-state))
        (.removeEventListener expand "scroll" (:on-expand-scroll element-state))
        (.removeChild element container)
        (aset element "_erd" nil))
      (set! (.-_erd element) (update (.-_erd element) :listeners (fn [listeners] (remove #{on-resize} listeners)))))))

(when (nil? (:scrollbar-size @system-state-atom))
  (add-pending-operation! {:operation :write-dom
                           :execute!  (fn parent-size-1 []
                                        (let [width 500
                                              height 500
                                              child (js/document.createElement "div")
                                              container (js/document.createElement "div")]
                                          (set! (.-cssText (.-style child))
                                                (str "position: absolute; width: " (* 2 width) "px; height: " (* 2 height) + "px; visibility: hidden; margin: 0; padding: 0;"))
                                          (set! (.-cssText (.-style container))
                                                (str "position: absolute; width: " width "px; height: " height "px; overflow: scroll; visibility: none; top: " (- (* 3 width)) + "px; left: " (- (* 3 height)) "px; visibility: hidden; margin: 0; padding: 0;"))
                                          (.appendChild container child)
                                          (swap! system-state-atom assoc :scrollbar-size-measurer-div container)
                                          (js/document.body.insertBefore container (.-firstChild (.-body js/document)))
                                          (add-pending-operation! {:operation :read-dom
                                                                   :execute!  (fn parent-size-2 []
                                                                                (let [scrollbar-width (- width (.-clientWidth container))
                                                                                      scrollbar-height (- height (.-clientHeight container))]
                                                                                  (swap! system-state-atom assoc :scrollbar-size {:width  scrollbar-width
                                                                                                                                  :height scrollbar-height})
                                                                                  (add-pending-operation! {:operation :write-dom
                                                                                                           :execute!  (fn parent-size-3 []
                                                                                                                        (js/document.body.removeChild container)
                                                                                                                        (check-system-ready!))})))})))}))
(defn get-size-input-value [state dimension]
  (cond (= dimension :width)
        {:width (:width state)}
        (= dimension :height)
        {:height (:height state)}
        :else
        {:width  (:width state)
         :height (:height state)}))

(def definition
  {:name         "parent-size"
   :get-instance (fn [{on-state-changed :on-state-changed
                       dimension        :dimension
                       should-update?   :should-update?
                       root-element     :root-element}]
                   (let [should-update? (or should-update? (fn [] true))
                         state-atom (atom {:width  nil
                                           :height nil})
                         on-resize-listener (fn [{width :width height :height}]
                                              (when (and (should-update?)
                                                         (or (not= width (:width @state-atom))
                                                             (not= height (:height @state-atom))))
                                                (swap! state-atom (fn [state]
                                                                    (merge state {:width  width
                                                                                  :height height})))
                                                (on-state-changed)))]
                     {:ready?       (fn []
                                      (not (nil? (get-size-input-value @state-atom dimension))))
                      :get-value    (fn []
                                      (get-size-input-value @state-atom dimension))
                      :did-mount    (fn [{element :element}]
                                      (install! {:element   element
                                                 :on-resize (fn [x]
                                                              (on-resize-listener x))})
                                      {:operation :read-dom
                                       :execute!  (fn []
                                                    (swap! state-atom (fn [state]
                                                                        (merge state {:width  (.-offsetWidth element)
                                                                                      :height (.-offsetHeight element)})))
                                                    (on-state-changed))})
                      :will-unmount (fn [{element :element}]
                                      (handle-unmount! {:element      element
                                                        :root-element root-element
                                                        :on-resize    on-resize-listener}))}))})
