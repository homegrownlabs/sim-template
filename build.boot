(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[org.clojure/clojure "1.6.0"]

                  ;; Simulant & Friends
                  [com.datomic/simulant "0.1.7" :exclusions [com.datomic/datomic-free]]
                  [org.craigandera/causatum "0.3.0"]

                  ;; Datomic & Friends
                  [com.datomic/datomic-free "0.9.5130" :exclusions [joda-time
                                                                    org.slf4j/slf4j-nop
                                                                    org.slf4j/slf4j-log4j12]]
                  [io.rkn/conformity "0.3.2" :exclusions [com.datomic/datomic-free]]

                  ;; HTTP Client
                  [clj-http "1.0.1"]

                  ;; JSON Decoding
                  [cheshire "5.3.1"]

                  ;; Environment Variables
                  [environ "0.5.0"]

                  ;; Logging
                  [adzerk/boot-logservice "1.0.0"]
                  [org.clojure/tools.logging "0.3.1"]
                  [org.slf4j/slf4j-api "1.7.6"]
                  [ch.qos.logback/logback-classic "1.1.1"]])

;; Load Datomic data readers
(#'clojure.core/load-data-readers)
(set! *data-readers* (.getRawRoot #'*data-readers*))


(require '[datomic.api :as d])

(deftask bootstrap
  "Bootstrap a Datomic database with simulation schema."
  [d uri          URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f bootstrap-fn FN  sym "Fully-qualified database bootstrap function that takes a single URI argument (e.g. simulation.db/bootstrap!)"]
  (if (and uri bootstrap-fn)
    (do
      (require (symbol (namespace bootstrap-fn)))
      ((resolve bootstrap-fn) uri))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not bootstrap-fn (boot.util/fail "The --bootstrap-fn option is required!\n"))
      (*usage*))))

(deftask create-model
  "Create a model to generate a test against."
  [d uri         URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f create-fn   FN  sym "Fully-qualified model create function that takes four options; uri, name, description and type (e.g. simulation.model/create-model!)."
   n name        VAL str "Model name."
   t type        KW  kw  "Fully-qualified model type (e.g. :model.type/sample)."
   _ description VAL str "Description of model (optional)."]
  (if (and uri create-fn name type)
    (do
      (require (symbol (namespace create-fn)))
      ((resolve create-fn) uri name description type))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not create-fn (boot.util/fail "The --create-fn option is required!\n"))
      (if-not name (boot.util/fail "The --name option is required!\n"))
      (if-not type (boot.util/fail "The --type option is required!\n"))
      (*usage*))))

(deftask list-models
  "List the available models in the database."
  [d uri    URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f list-fn FN  sym "Fully qualified function that returns model entities (takes: db)"]
  (if (and uri list-fn)
    (let [db     (d/db (d/connect uri))
          _      (require (symbol (namespace list-fn)))
          models (->> ((resolve list-fn) db)
                      (map #(select-keys % [:model/name])))]
      (if (seq models)
        (clojure.pprint/print-table models)
        (println "No models in database.")))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not list-fn (boot.util/fail "The --list-fn option is required!\n"))
      (*usage*))))

(deftask create-test
  "Create a test from a given model."
  [d uri         URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f create-fn   FN  sym "Fully qualified function that creates a test (takes: uri, model-name, test-name, duration, agent-count)"
   m model-name  VAL str "The model name to generate the test from"
   n test-name   VAL str "The name of the test"
   u host-name   URI str "URI of the target system (e.g. http://dockerhost:8080)"
   t duration    MIN int "The number of minutes to run"
   c concurrency N   int "The number of agents to run simultaneously"]
  (if (and uri model-name test-name host-name duration concurrency)
    (do
      (require (symbol (namespace create-fn)))
      ((resolve create-fn) uri model-name test-name host-name duration concurrency))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not create-fn (boot.util/fail "The --create-fn option is required!\n"))
      (if-not model-name (boot.util/fail "The --model-name option is required!\n"))
      (if-not test-name (boot.util/fail "The --test-name option is required!\n"))
      (if-not host-name (boot.util/fail "The --host-name option is required!\n"))
      (if-not duration (boot.util/fail "The --duration option is required!\n"))
      (if-not concurrency (boot.util/fail "The --concurrency option is required!\n"))
      (*usage*))))

(deftask list-tests
  "List the available tests in the database."
  [d uri    URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f list-fn FN  sym "Fully qualified function that returns test entities (takes: db)"]
  (if (and uri list-fn)
    (let [db     (d/db (d/connect uri))
          _      (require (symbol (namespace list-fn)))
          tests (->> ((resolve list-fn) db)
                      (map #(select-keys % [:test/name])))]
      (if (seq tests)
        (clojure.pprint/print-table tests)
        (println "No tests in database.")))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not list-fn (boot.util/fail "The --list-fn option is required!\n"))
      (*usage*))))

(deftask run-sim
  "Run a sim from a given test."
  [d uri         URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f run-fn      FN  sym "Fully-qualified function that runs a sim (takes: uri, test-name, processes, acceleration-factor)."
   n test-name   VAL str "Test name to simulate."
   p processes   N   int "How many Clojure agents will host the sim agents?"
   s speed       N   int "Speed to run the sim at (e.g. '2' means 2x as fast). "]
  (if (and uri run-fn test-name processes speed)
    (do
     (require (symbol (namespace run-fn)))
     ((resolve run-fn) uri test-name processes speed))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not run-fn (boot.util/fail "The --run-fn option is required!\n"))
      (if-not test-name (boot.util/fail "The --test-name option is required!\n"))
      (if-not processes (boot.util/fail "The --processes option is required!\n"))
      (if-not speed (boot.util/fail "The --speed option is required!\n"))
      (*usage*))))

(deftask list-sims
  "List the available sims in the database."
  [d uri       URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f list-fn   FN  sym "Fully qualified function that returns sim entities (takes: db, test-query <may be nil>)"
   n test-name VAL str "Test name to limit results by."]
  (if (and uri list-fn)
    (let [db     (d/db (d/connect uri))
          _      (require (symbol (namespace list-fn)))
          sims   (->> ((resolve list-fn) db test-name)
                      (sort-by :created))]
      (if (seq sims)
        (clojure.pprint/print-table [:sim :test-name :created] sims)
        (println (str "No sims in database"
                      (if test-name
                        (str "(for test " test-name ").")
                        ".")))))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not list-fn (boot.util/fail "The --list-fn option is required!\n"))
      (*usage*))))

(deftask validate-sim
  "Validate a given sim."
  [d uri         URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f validate-fn FN  sym "Fully-qualified function that validates a sim (takes: uri, sim-id)."
   s sim-id      N   int "ID of the simulation"]
  (if (and uri validate-fn sim-id)
    (do
      (require (symbol (namespace validate-fn)))
      (let [errors ((resolve validate-fn) uri sim-id)]
        (if (seq errors)
          (do
            (println (str "Sim " sim-id " had validation errors:"))
            (clojure.pprint/pprint errors))
          (println (str "Sim " sim-id " had no validation errors.")))))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not validate-fn (boot.util/fail "The --validate-fn option is required!\n"))
      (if-not sim-id (boot.util/fail "The --sim-id option is required!\n"))
      (*usage*))))


(deftask validate-latest
  "Validate the latest sim for a given test-name."
  [d uri         URI str "Datomic database URI (e.g. \"datomic:free://localhost:4334/sample-sim\")"
   f validate-fn FN  sym "Fully-qualified function that validates a sim (takes: uri, sim-id)."
   n test-name   VAL str "Test name to look-up sims by."
   l lookup-fn   FN  sym "Fully-qualified function that looks up latest sim for test-name (takes: db, test-name)." ]
  (if (and uri validate-fn test-name lookup-fn)
    (do
      (require (symbol (namespace validate-fn)))
      (let [db     (d/db (d/connect uri))
            sim-id ((resolve lookup-fn) db test-name)
            errors ((resolve validate-fn) uri sim-id)]
        (if (seq errors)
          (do
            (println (str "Sim " sim-id " had validation errors:"))
            (clojure.pprint/pprint errors))
          (println (str "Sim " sim-id " had no validation errors.")))))
    (do
      (if-not uri (boot.util/fail "The --uri option is required!\n"))
      (if-not validate-fn (boot.util/fail "The --validate-fn option is required!\n"))
      (if-not test-name (boot.util/fail "The --test-name option is required!\n"))
      (if-not lookup-fn (boot.util/fail "The --lookup-fn option is required!\n"))
      (*usage*))))


(comment
  (bootstrap
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.db/bootstrap!"
    )

  (create-model
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.model/create-model!"
    "-n" "Sample Model"
    "-t" "model.type/sample"
    )

  (list-models
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.model/list-models"
    )

  (create-test
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.test/create-test!"
    "-m" "Sample Model"
    "-n" "Sample Test"
    "-u" "http://dockerhost:8080"
    "-t" "1"
    "-c" "1"
    )

  (list-tests
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.test/list-tests"
    )

  (run-sim
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.sim/run-sim!"
    "-n" "Sample Test"
    "-p" "1"
    "-s" "1"
    )

  (list-sims
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.sim/list-sims"
    )

  (validate-sim
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.validations/validate"
    "-s" "457396837156256"
    )

  (validate-latest
    "-d" "datomic:free://localhost:4334/sample-sim"
    "-f" "simulation.validations/validate"
    "-l" "simulation.sim/latest-sim"
    "-n" "Sample Test"
    )
  )
