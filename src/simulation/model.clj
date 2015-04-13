(ns simulation.model
  "This namespace describes potential agent interactions with a system, as well
  as functions for realizing that non-deterministic behavior into a fixed
  simulant 'test'."
  (:require [simulant.util :refer [tx-ent]]
            [datomic.api :as d]
            [causatum.event-streams :as es]
            [simulation.util :as util]))

(def agent-behavior
 ;; from-state to-state  weight  max-delay (ms)
 #{[:start     :retrieve 100     100]
   [:retrieve  :retrieve 33      50]
   [:retrieve  :inc      33      50]
   [:retrieve  :dec      33      50]
   [:inc       :retrieve 20      50]
   [:inc       :inc      60      50]
   [:inc       :dec      20      50]
   [:dec       :retrieve 20      50]
   [:dec       :dec      60      50]
   [:dec       :inc      20      50]})

(defn delay->qualified-delay
  "Convert an integer max-delay delay into a causatum-qualified :random delay."
  [delay]
  [:random delay])

(defn edge->node
  "Convert an edge-vector into a causatum node."
  [edge]
  (let [[_ to weight delay] edge]
    {to {:weight weight
         :delay (delay->qualified-delay delay)}}))

(defn edge-graph->node-graph
  "Transform a set of edges into a causatum-compatible node graph."
  [edges]
  (->> edges
       (group-by first)
       (map (fn [[from tos]] [from (->> tos
                                        (mapv edge->node)
                                        (into {})
                                        vector)]))
       (into {})))

(defn state-transition-model
  "Construct a causatum state-transition model from a set of edges."
  [edges]
  {:graph (edge-graph->node-graph edges)
   :delay-ops {:random (fn [rtime n] (rand n))}})

(def initial-state
  [{:state :start
    :rtime 0}])

(defn state-stream
  "Produce a lazy sequence of states for a set of edges describing a state
  machine."
  [edges]
  (es/event-stream (state-transition-model edges)
                   initial-state))

(defn find-by-name [db name]
  (d/entity db [:model/name name]))

(defn create-model!
  "Persist a model and its metadata to the database"
  [uri name description type]
  (let [conn     (d/connect uri)
        model-id (d/tempid :model)
        model    (cond-> {:db/id model-id
                          :model/type :model.type/sample
                          :model/name name
                          :source/codebase (util/codebase-ent :model)}

                   description
                   (assoc  :model/description description))]
    (-> @(d/transact conn [model])
        (tx-ent model-id))))

(defn list-models
  [db]
  (->> (d/q '[:find ?m
              :where [?m :model/name]]
            db)
       (map first)
       (map (partial d/entity db))))
