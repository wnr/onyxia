(ns onyxia.output-definitions)

(def definitions (atom {}))

(defn add! [definition]
  (swap! definitions assoc (:name definition) definition))

(defn get! [name]
  (get @definitions name))
