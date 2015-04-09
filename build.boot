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
