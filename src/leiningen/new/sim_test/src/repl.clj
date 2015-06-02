(ns {{namespace}}.repl
  "REPL-driven example of creating and running a sim."
  (:require [datomic.api :as d]
            [{{namespace}}.db :as db]
            [{{namespace}}.model :refer [create-model!]]
            [{{namespace}}.test :refer [create-test!]]
            [{{namespace}}.sim :refer [run-sim!] :as sim]
            [{{namespace}}.validations :refer [validate]])
  (:refer-clojure :exclude [test]))

(def model-name "repl-model")
(def test-name "repl-test")
(def sim-acceleration-factor 1.0)
(def sim-agent-count 1)

(def uri (str "datomic:mem://" (d/squuid)))

(comment
  (d/delete-database uri))

;; 0. Create and bootstrap a database (happens once)
(db/bootstrap! uri)
;; => nil

;; 1. Create a model. Generally only happens when we tweak/refine our
;;    behavioral model.
(def model (create-model! uri model-name "Created by example.clj" :model.type/sample))
;; => {:db/id 123456789}

;; 2. Generate a test from that model. Happens slightly more frequently as we
;;    create different configurations of tests to run. For example, we may test
;;    with few acquirers/users/time to get a quick feel, or many to stress the
;;    system.
(def test (create-test! uri model-name test-name
                        "http://dockerhost:8080"
                        1 ;; duration (minutes)
                        sim-agent-count))
;; => {:db/id 223456789}

;; 3. Run a sim. This happens frequently, probably multiple times per test.
(def sim (run-sim! uri test-name sim-agent-count sim-acceleration-factor))
;; => {...<sim context map>...}

;; 4. Validate the sim. Returns any validation errors.
(def latest-sim (sim/latest-sim (d/db (d/connect uri)) test-name))
(def errors (validate uri latest-sim))
(when-not (empty? errors) (clojure.pprint/pprint errors))
