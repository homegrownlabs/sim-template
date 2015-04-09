(ns simulation.actions.sample
  "Agent actions for the included sample service. These functions are where the
  rubber meats the road and your sim communicates with the target system."
  (:require [clojure.tools.logging :as l]
            [datomic.api :as d]
            [simulant.sim :as sim]
            [simulant.util :as util :refer [e solo]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [simulation.actions :refer [log log-error timed]]))

(defn retrieve-count [host]
  (http/get host))

(defn increment-count [host]
  (http/put (str host "/inc")))

(defn decrement-count [host]
  (http/put (str host "/dec")))

(defmethod sim/perform-action :action.type/retrieve-count [action process]
  (try
    (l/info "Starting Retrieve")
    (let [agent        (-> action :agent/_actions solo)
          host         (-> agent :test/_agents solo :test/host)
          [resp nsecs] (timed (retrieve-count host))]
      (log action process resp nsecs))
    (catch Throwable t
      (l/error t "Exception during :action.type/retrieve-count")
      (log-error action process t 0))))

(defmethod sim/perform-action :action.type/increment-count [action process]
  (try
    (l/info "Starting Increment")
    (let [agent        (-> action :agent/_actions solo)
          host         (-> agent :test/_agents solo :test/host)
          [resp nsecs] (timed (increment-count host))]
      (log action process resp nsecs))
    (catch Throwable t
      (l/error t "Exception during :action.type/increment-count")
      (log-error action process t 0))))

(defmethod sim/perform-action :action.type/decrement-count [action process]
  (try
    (l/info "Starting Decrement")
    (let [agent        (-> action :agent/_actions solo)
          host         (-> agent :test/_agents solo :test/host)
          [resp nsecs] (timed (decrement-count host))]
      (log action process resp nsecs))
    (catch Throwable t
      (l/error t "Exception during :action.type/decrement-count")
      (log-error action process t 0))))
