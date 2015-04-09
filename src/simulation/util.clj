;; TODO: Consider pulling this into a library
(ns simulation.util
  (:require [simulant.util :refer [git-repo-uri git-latest-sha]]
            [datomic.api :as d]))

(defn codebase-ent
  "Return a codebase entity in a given partition (part) to be transacted."
  ([part] (codebase-ent part (git-repo-uri) (git-latest-sha)))
  ([part uri sha]
   {:db/id (d/tempid part)
    :repo/type :repo.type/git
    :git/uri uri
    :git/sha sha}))
