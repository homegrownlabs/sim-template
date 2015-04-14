(ns {{namespace}}.db
  "Bootstrap functions to install schema into the backing Datomic store."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as l]
            [datomic.api :as d]
            [io.rkn.conformity :as c]))

(defn- load-schema
  "Load an edn schema resource file, reading it as Clojure data."
  [resource-filename]
  (->> resource-filename
      io/resource
      slurp
      read-string))

(defn bootstrap!
  "Bootstrap schema into the database."
  [uri]
  (l/info "Bootstrapping database" uri)
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (doseq [rsc ["simulant.edn" ;; Simulant's base schema
                 "schema.edn"]] ;; Your own schema extensions
      (let [norms (load-schema rsc)]
        (c/ensure-conforms conn norms)))))
