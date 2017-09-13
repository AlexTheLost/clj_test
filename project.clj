(defproject clj_test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yesql "0.5.3"]
                 [mysql/mysql-connector-java "5.1.32"]

                 [compojure "1.6.0"]
                 [org.immutant/web "2.1.9"]
                 [clj-http "3.7.0"]
                 [org.clojure/data.json "0.2.6"]

                 [clj-time "0.14.0"]
                 [jarohen/chime "0.2.2"]

                 ;; DEV:
                 [ring/ring-devel "1.1.8"]
                 ]
  :main ^:skip-aot clj-test.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
