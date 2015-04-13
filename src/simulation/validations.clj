(ns simulation.validations
  "Queries used to validate a completed sim run."
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [simulant.util :refer [solo e]]))

;; == Validations ==========================================

(defn- some-actions-logged
  "Validate that the sim contains at least one logged action."
  [db sim]
  (when-not (seq (:actionLog/_sim sim))
    [{:validation :some-actions-logged
      :error "No actions were logged for sim"}]))

(defn- no-exceptions-thrown
  "Validate that no actions resulted in an exception."
  [db sim]
  (let [exceptions (d/q '[:find ?time ?log ?action ?type ?etype ?emsg
                          :in $ ?sim
                          :where
                          [?log :actionLog/sim ?sim ?tx]
                          [?log :actionLog/action ?action]
                          [?log :actionLog/exceptions ?exc]
                          [?exc :exception/type ?etype]
                          [?exc :exception/message ?emsg]
                          [?action :action/atTime ?time]
                          [?action :action/type ?type]]
                        db (e sim))]
    (for [[time log action-id type etype emsg] (sort-by first exceptions)]
      {:validation :no-exceptions-thrown
       :action-type type
       :action-log-id log
       :action-id action-id
       :at-time time
       :exception-type etype
       :exception-message emsg})))

(defn- s->ns [s]
  (* s 1000000000))

(defn- actions-took-less-than
  "Return a validator that ensures all requests completed in less than s
  seconds."
  [s]
  (fn [db sim]
    (for [[log action type expected-ns actual-ns]
          (d/q '[:find ?log ?action ?type ?expected ?actual
                 :in $ ?sim ?expected
                 :where
                 [?log :actionLog/sim ?sim]
                 [?log :actionLog/action ?action]
                 [?log :actionLog/nsec ?actual]
                 [(> ?actual ?expected)]
                 [?log :actionLog/action ?action]
                 [?action :action/type ?type]]
               db (e sim) (s->ns s))]
      {:validation :actions-took-less-than
       :error (str "Action " (->> action (d/entity db) :action/type) " took " actual-ns "ns, "
                   "but should have been less than " expected-ns)
       :action-type type
       :action-id action
       :action-log-id log
       :expected-ns expected-ns
       :actual-ns actual-ns})))


(defn- extract-count
  "Extract the 'count' value from a retrieve-count action log's response."
  [db action-log-id]
  (-> (d/entity db action-log-id)
      :actionLog/responseMap
      edn/read-string
      :body
      json/parse-string
      (get "count")))

(defn- count-actions-of-type
  "Count occurences of actions of a given type"
  [db sim type]
  (ffirst (d/q '[:find (count ?log)
                 :in $ ?sim ?type
                 :where
                 [?log :actionLog/sim ?sim]
                 [?log :actionLog/action ?action]
                 [?action :action/type ?type]]
               db (e sim) type)))

(defn- count-matches
  "Validate the final count from the system reflects the sum of the original,
  increment, and decrement calls"
  [db sim]
  (let [actions (->> (d/q '[:find ?log ?at
                            :in $ ?sim
                            :where
                            [?log :actionLog/sim ?sim]
                            [?log :actionLog/action ?action]
                            [?action :action/atTime ?at]
                            [?action :action/type :action.type/retrieve-count]]
                          db (e sim))
                     (sort-by second) ;; Sort-by time
                     (map first))     ;; Return action-log IDs
        start-count (extract-count db (first actions))
        end-count   (extract-count db (last actions))
        incs        (count-actions-of-type db sim :action.type/increment-count)
        decs        (count-actions-of-type db sim :action.type/decrement-count)
        expected    (+ start-count incs (- decs))]
    (when-not (= end-count expected)
      {:validation :count-matches
       :error (str "Expected final count to be " expected ", but it was " end-count)
       :expected expected
       :actual end-count})))

;; == Validation Runner ====================================

(defmulti validations-for :test/type)

(defmethod validations-for :default [type]
  (throw (ex-info (str  "No validations for test type " type) {:test/type type})))

(defmethod validations-for :test.type/sample [_]
  [some-actions-logged
   no-exceptions-thrown
   (actions-took-less-than 1)
   count-matches])

(defn validate
  "Validate a sim passes validations. Returns a sequence of errors"
  [uri sim-id]
  (let [db          (-> uri d/connect d/db)
        sim         (d/entity db sim-id)
        validations (validations-for (-> sim :test/_sims solo))]
    (mapcat (fn [v] (v db sim)) validations)))
