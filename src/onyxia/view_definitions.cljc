(ns onyxia.view-definitions
  (:require [ysera.error :refer [error]]))

(def definitions (atom {}))

(defn get! [name]
  (get @definitions name))

(defn add! [definition]
  (let [name (:name definition)]
    (when (get! name)
      ;;(error "view definition name collision: " name)
      ;; TODO: What should happen
      nil)
    (swap! definitions assoc name definition)))

(defn add-with-dependencies! [definition]
  (add! definition)
  (when-let [dependencies (:dependencies definition)]
    (reduce (fn [_ dependency]
              (add-with-dependencies! dependency))
            nil
            dependencies)))
