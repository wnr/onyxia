(ns onyxia.attributes-utils
  (:require [ysera.test #?(:clj :refer :cljs :refer-macros) [is=]]
    ;[camel-snake-kebab.core :refer [->camelCase]]
            [onyxia.attributes-map :refer [kebab->camel]]))

(defn replace-key
  ([map key new-key]
   (replace-key map key new-key (get map key)))
  ([map key new-key value]
   (if (contains? map key)
     (-> (dissoc map key)
         (assoc new-key value))
     map)))

(defn replace-value
  [map key value]
  (if (contains? map key)
    (update map key value)
    map))

(defn formalize-event-handlers
  {:test (fn []
           (is= (formalize-event-handlers nil) [])
           (is= (formalize-event-handlers []) [])
           (is= (formalize-event-handlers [:foo]) [[:foo]])
           (is= (formalize-event-handlers [{} :foo]) [[{} :foo]])
           (is= (formalize-event-handlers +) [+])
           (is= (formalize-event-handlers [[:foo]]) [[:foo]])
           (is= (formalize-event-handlers [[{} :foo]]) [[{} :foo]])
           (is= (formalize-event-handlers [+]) [+]))}
  [handlers]
  (if (or (and (coll? handlers)
               (not (empty? handlers))
               (or (map? (first handlers))
                   (keyword? (first handlers))))
          (and (not (coll? handlers))
               (not (nil? handlers))))
    [handlers]
    (or handlers [])))

(defn change-attribute
  {:test (fn []
           (is= (change-attribute {:foo :bar} {:key :baz :new-key :xar}) {:foo :bar})
           (is= (change-attribute {:foo :bar} {:key :foo :new-key :baz}) {:baz :bar})
           (is= (change-attribute {:foo :bar} {:key :foo :assoc "hello"}) {:foo "hello"})
           (is= (change-attribute {:foo 1} {:key :foo :update inc}) {:foo 2})
           (is= (change-attribute {:foo :bar} {:key :foo :new-key :bar :assoc "hello"}) {:bar "hello"})
           (is= (change-attribute {:foo 1} {:key :foo :new-key :bar :update inc}) {:bar 2})
           (is= (change-attribute {:foo 1 :bar 2} {:key :foo :new-key :bar :update inc :merge-fn +}) {:bar 4}))}
  [attributes {key         :key
               new-key     :new-key
               assoc-value :assoc
               update-fn   :update
               merge-fn    :merge-fn}]
  {:pre [(or (nil? assoc-value) (nil? update-fn))]}
  (if-not (contains? attributes key)
    attributes
    (as-> attributes $
          (if assoc-value
            (assoc $ key assoc-value)
            $)
          (if update-fn
            (update $ key update-fn)
            $)
          (if new-key
            (-> (dissoc $ key)
                (assoc new-key (if (and merge-fn (contains? $ new-key))
                                 (merge-fn (get attributes new-key) (get $ key))
                                 (get $ key))))
            $))))

(defn map-default-attribute-events
  [attrs {on-dom-event   :on-dom-event
          attribute-keys :attribute-keys}]
  (let [handle-dom-event (fn [{attributes-key :attributes-key
                               type           :type
                               event          :event}]
                           (on-dom-event {:type      type
                                          :dom-event :event
                                          :event     event
                                          :handlers  (formalize-event-handlers (get attrs attributes-key))}))]
    (reduce (fn [attrs attribute-key]
              (change-attribute attrs {:key     attribute-key
                                       :new-key (kebab->camel attribute-key) ;(->camelCase attribute-key)
                                       :assoc   (fn [e] (handle-dom-event {:attributes-key attribute-key
                                                                           :type           attribute-key
                                                                           :event          e}))
                                       }))
            attrs
            attribute-keys)))