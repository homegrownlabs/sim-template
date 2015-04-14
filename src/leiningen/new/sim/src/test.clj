(ns {{namespace}}.test
  "This namespace contains the logic for realizing a non-deterministic model
  into a fixed test, consisting of test metadata, agents, and their planned
  actions.

  If any of your own actions include procedurally generated content, this is
  where you should realize that content. Clojure's clojure.data.generators
  contains a number of useful functions for generating content, but you will
  need to write your domain-specific generators."
  (:require [clojure.tools.logging :as l]
            [datomic.api :as d]
            [simulant.sim :as sim]
            [simulant.util :refer [e tx-ent]]
            [{{namespace}}.model :as m]
            [{{namespace}}.util :as util]))

(defn- action
  "Generate a Simulant action entity, given an agent, it's state, and type.
  Optionally accepts a map of additional Datomic attributes to include in the
  entity."
  ([agent state type]
   (action agent state type nil))
  ([agent state type extra-attrs]
   (merge {:db/id          (d/tempid :test)
           :agent/_actions (e agent)
           :action/atTime  (long (:rtime state))
           :action/type    type}
          extra-attrs)))

(defmulti actions-for
  "For a given state transition, return a collection of actions the agent
  should undertake."
  (fn [_ state] (:state state)))

(defmethod actions-for :start [agent state] [])

(defmethod actions-for :retrieve [agent state]
  [(action agent state :action.type/retrieve-count)])

(defmethod actions-for :inc [agent state]
  [(action agent state :action.type/increment-count)])

(defmethod actions-for :dec [agent state]
  [(action agent state :action.type/decrement-count)])


(defn- gen-test [model test]
  (assoc test
         :test/type    :test.type/sample
         :model/_tests (e model)))

(defn- gen-acq-agents
  [test]
  (map (fn [n]
         {:db/id          (d/tempid :test)
          :agent/type     :agent.type/robot
          :agent/number   n
          ;; Any of your own domain-specific agent attributes would go here.
          :test/_agents   (e test)})
       (range (:test/agentCount test))))

(defn gen-agent-actions
  "Generate a stream of actions for a single agent. Adds one additional
  :retrieve action 25ms after the conclusion of the agent's planned actions."
  [agent duration]
  (let [planned-actions (->> (m/state-stream m/agent-behavior)
                             (mapcat (partial actions-for agent))
                             (take-while #(<= (:action/atTime %) duration)))
        last-action-time (:action/atTime (last planned-actions))]
    (concat planned-actions
            (actions-for agent {:state :retrieve
                                :rtime (+ last-action-time 25)}))))

(defmethod sim/create-test :model.type/sample
  [conn model test-basis]
  (let [test    (gen-test model test-basis)
        agents  (gen-acq-agents test)
        actions (mapcat (fn [agent] (gen-agent-actions agent (:test/duration test)))
                        agents)]
    (-> @(d/transact conn (concat [test] agents actions))
        (tx-ent (e test)))))

(defn create-test!
  "Persist a test and its metadata to the database."
  [uri model-name test-name host duration agent-count]
  (let [conn     (d/connect uri)
        test-def {:db/id           (d/tempid :test)
                  :test/name       test-name
                  :source/codebase (util/codebase-ent :test)
                  :test/host       host
                  :test/duration   (* 1000 10 duration) ;; minutes -> msecs
                  :test/agentCount agent-count}
        model    (m/find-by-name (d/db conn) model-name)]
    (assert model (str "Model " model-name " must exist to create a test"))
    (sim/create-test conn model test-def)))

(defn find-by-name
  "Lookup a test entity by name."
  [db test-name]
  (d/entity db [:test/name test-name]))

(defn list-tests
  "Return a collection of tests contained in a database."
  [db]
  (->> (d/q '[:find ?m
              :where [?m :test/name]]
            db)
       (map first)
       (map (partial d/entity db))))
