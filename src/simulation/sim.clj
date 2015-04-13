(ns simulation.sim
  "This namespace contains the logic for creating and running a simulation."
  (:require [clojure.tools.logging :as l]
            [simulant.sim :as sim]
            [simulant.util :refer [e tx-ent]]
            [datomic.api :as d]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [simulation.test :as test]
            [simulation.util :as util]))

(defmethod sim/create-sim :test.type/sample
  [conn test sim]
  (-> @(d/transact conn (sim/construct-basic-sim test sim))
      (tx-ent (e sim))))

(defn find-test [context]
  (let [{:keys [conn test-name]} context
        test                     (test/find-by-name (d/db conn) test-name)]
    (assert test (str "Test with name " test-name " must exist."))
    (assoc context :test test)))

(defn- retrieve-codebase-ent [host]
  (l/info (str "Retrieving codebase info for " host))
  (let [resp (http/get (str host "/version"))
        body (json/parse-string (:body resp) keyword)]
    (assoc (util/codebase-ent :sim (:repo body) (:sha body))
           :codebase/host host)))

(defn- setup-sim
  "Create and setup a sim, capturing metadata about the system under test and
  creating associated internal entities."
  [context]
  (l/info "Setting up sim")
  (let [{:keys [conn test process-count clock-multiplier]} context
        sim-def    {:db/id            (d/tempid :sim)
                    :source/codebase  (util/codebase-ent :sim)
                    :sim/codebases    [(retrieve-codebase-ent (:test/host test))]
                    :sim/processCount process-count}
        sim        (sim/create-sim conn test sim-def)]
    (sim/create-action-log    conn sim)
    (sim/create-process-state conn sim)
    (sim/create-fixed-clock   conn sim {:clock/multiplier clock-multiplier})
    (assoc context :sim sim)))

(defn- setup-system
  "Perform miscellaneous setup actions on the target system"
  [context]
  (l/info "Setting up target system")
  ;; Create a simulation.target-system namespace, and perform any necessary
  ;; actions on it. Make sure to return an original or modified context from
  ;; those functions.
  ;;
  ;; For example:
  ;; (-> context
  ;;     target-system/create-accounts)
  context)

(defn- setup-actions
  "Require action namespaces, so their multimethod definitions get loaded."
  [context]
  (require 'simulation.actions.sample)
  context)

(defn- start-sim
  "Initiate processes for sim, effectively starting the sim."
  [context]
  (let [{:keys [uri sim]} context
        run-process-fn    #(sim/run-sim-process uri (e sim))
        _                 (l/info (str "Launching " (:sim/processCount sim) " sim processes"))
        processes         (repeatedly (:sim/processCount sim) run-process-fn)]
    (doall processes)
    (assoc context :processes processes)))

(defn- await-sim
  "Wait for all sim processes to complete."
  [context]
  (l/info "Awaiting sim processes...")
  (doseq [p (:processes context)]
    @(:runner p))
  (l/info "Sim complete")
  context)

(defn run-sim!
  "Setup and run a sim. Awaits completion of each agent process."
  [uri test-name process-count clock-multiplier]
  (-> {:uri uri
       :conn (d/connect uri)
       :test-name test-name
       :process-count process-count
       :clock-multiplier clock-multiplier}
      find-test
      setup-sim
      setup-system
      setup-actions
      start-sim
      await-sim))

;; == Sim retrieval ========================================

(defn all-sims [db]
  (->> (d/q '[:find ?sim ?test-name ?created
              :where
              [?test :test/sims ?sim ?tx]
              [?test :test/name ?test-name]
              [?tx :db/txInstant ?created]]
            db)
       (map #(zipmap [:sim :test-name :created] %))))

(defn sims-for-test [db test-name]
  (->> (d/q '[:find ?sim ?test-name ?created
              :in $ ?test
              :where
              [?test :test/sims ?sim ?tx]
              [?test :test/name ?test-name]
              [?tx :db/txInstant ?created]]
            db [:test/name test-name])
       (map #(zipmap [:sim :test-name :created] %))))

(defn list-sims
  "Return listing of sims in database."
  [db test-name]
  (if test-name
    (sims-for-test db test-name)
    (all-sims db)))

(defn latest-sim [db test-name]
  (->> (sims-for-test db test-name)
       (sort-by :created)
       last
       :sim))
