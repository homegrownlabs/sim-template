(ns leiningen.new.sim-test
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files sanitize-ns project-name]]
            [leiningen.core.main :as main]))

(def render (renderer "sim-test"))

(defn sim-test
  "Generate a sim-test project"
  [name]
  (let [sanitized-ns (sanitize-ns name)
        data {:raw-name name
              :name (project-name name)
              :namespace sanitized-ns
              :sanitized (name-to-path sanitized-ns)
              :namespace-set (str "'#{" namespace "}")}]
    (main/info "Generating fresh 'lein new' sim-test project.")
    (->files data
             ["README.md" (render "README.md" data)]
             ["build.boot" (render "build.boot" data)]
             [".gitignore" (render ".gitignore" data)]
             ["run-service.sh" (render "run-service.sh" data)]

             ["src/{{sanitized}}/actions.clj" (render "src/actions.clj" data)]
             ["src/{{sanitized}}/actions/sample.clj" (render "src/actions/sample.clj" data)]
             ["src/{{sanitized}}/db.clj" (render "src/db.clj" data)]
             ["src/{{sanitized}}/model.clj" (render "src/model.clj" data)]
             ["src/{{sanitized}}/repl.clj" (render "src/repl.clj" data)]
             ["src/{{sanitized}}/sim.clj" (render "src/sim.clj" data)]
             ["src/{{sanitized}}/test.clj" (render "src/test.clj" data)]
             ["src/{{sanitized}}/util.clj" (render "src/util.clj" data)]
             ["src/{{sanitized}}/validations.clj" (render "src/validations.clj" data)]

             ["resources/logback.xml" (render "resources/logback.xml" data)]
             ["resources/schema.edn" (render "resources/schema.edn" data)]
             ["resources/simulant.edn" (render "resources/simulant.edn" data)])))

