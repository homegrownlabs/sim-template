(ns simulation.actions
  "Support for actions, incl. logging success or failure to the action log."
  (:require [simulant.sim :as sim]
            [simulant.util :refer [e solo only]]
            [datomic.api :as d]
            [cheshire.core :as json]))

(defmacro timed
  "Evaluate expression, returning a pair of [result nsecs-taken]"
  [expr]
  `(let [start# (System/nanoTime)
         result# ~expr
         nsecs# (- (System/nanoTime)
                   start#)]
     [result# nsecs#]))

(defn- ex-data-trimmed
  "Remove known anonymous functions in exception data (clj-http's `client`, in
  particular)"
  [ex]
  (let [{:keys [environment] :as data} (ex-data ex)]
    (-> (ex-data ex)
        (assoc :environment (dissoc (:environment ex) (symbol "client"))))))

(defn- log-entity [action process response nsec extras]
  (merge
    (cond-> {:db/id (d/tempid :log)
             :actionLog/action (e action)
             :action/type (:action/type action)
             :actionLog/nsec nsec
             :actionLog/sim (e (-> process :sim/_processes only))}

      response
      (assoc :actionLog/responseMap (pr-str response)
             :actionLog/responseCode (:status response))

      (get-in response [:headers "content-type"])
      (assoc :actionLog/contentType (get-in response [:headers "content-type"])))
    extras))

(defn- stack-trace [ex]
  (let [s (java.io.StringWriter.)
        p (java.io.PrintWriter. s)]
    (.printStackTrace ex p)
    (str s)))

(defn- exception-entity
  [ex]
  (cond-> {:exception/type (.getName (class ex))
           :exception/message (or (.getMessage ex) "No message available.")
           :exception/stackTrace (stack-trace ex)}

          (ex-data ex)
          (assoc :exception/exceptionData (json/generate-string (ex-data-trimmed ex)))

          (.getCause ex)
          (assoc :exception/cause (exception-entity (.getCause ex)))))

(defn log [action process response nsec & {:as extras}]
  (let [log-fn (get sim/*services* :simulant.sim/actionLog)]
    (log-fn [(log-entity action process response nsec extras)])))

(defn log-error [action process exception nsec & {:as extras}]
 (let [log-fn (get sim/*services* :simulant.sim/actionLog)
       ex-ent (assoc extras :actionLog/exceptions (exception-entity exception))
       resp   (get (ex-data exception) :object)]
   (log-fn [(log-entity action process resp nsec ex-ent)])))

