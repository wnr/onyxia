(ns example-app.animation.sin-wave
  (:require [ysera.math :refer [sin floor]]))

(def bar-count 128)

(defn get-bar-element [i tick width]
  (let [translate-y (* (sin (+ (/ tick 10)
                               (/ i 5)))
                       100
                       0.5)
        hue (mod (- (* (/ 360 bar-count) i) tick) 360)
        color (str "hsl(" hue ",95%,55%)")
        rotation (mod (+ tick i) 360)
        bar-count (min 200 (floor (/ width 15)))
        bar-width (/ 100 bar-count)
        bar-x (* bar-width i)]
    [:div {:style {:position         "absolute"
                   :height           "100%"
                   :border-radius    "50%"
                   :max-width        "10px"
                   :width            (str bar-width "%")
                   :left             (str bar-x "%")
                   :transform        (str "scale(0.8,0.5) translateY(" translate-y "%) rotate(" rotation "deg)")
                   :background-color color}}]))

(def view
  {:name   "sin-wave"
   :input  [{:name      "animation"
             :input-key :animation}
            {:name      "parent-size"
             :dimension :width
             :input-key :size}]
   :render (fn [{{event         :event
                  tick          :tick
                  delta-time-ms :delta-time-ms} :animation
                 {width :width}                 :size}]
             [:div
              [:div {:style {:height   "150px"
                             :position "relative"
                             :overflow "hidden"}}
               (map (fn [i]
                      (get-bar-element i tick width))
                    (range bar-count))
               ;(loop [a '()
               ;       i 127]
               ;  (if (>= i 0)
               ;    (recur  (dec i))
               ;    a))
               ]
              [:p "dt: " delta-time-ms]]
             )})