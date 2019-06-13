(defproject http.async.client :lein-v
  :name             "http.async.client"
  :description      "Asynchronous HTTP Client for Clojure"
  :url              "https://github.com/cch1/http.async.client"
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.ning/async-http-client "1.9.40"]
                 [org.clojure/tools.logging "0.4.1"]]
  :min-lein-version "2.5.1"
  :plugins [[codox "0.8.12"]
            [lein-difftest "2.0.0"]
            [com.roomkey/lein-v "7.0.0"]]
  :middleware [lein-v.plugin/middleware]
  :profiles {:1.3.0 {:dependencies [[org.clojure/clojure "1.3.0" :upgrade? false]]}
             :1.4.0 {:dependencies [[org.clojure/clojure "1.4.0" :upgrade? false]]}
             :1.5.1 {:dependencies [[org.clojure/clojure "1.5.1" :upgrade? false]]}
             :1.6.0 {:dependencies [[org.clojure/clojure "1.6.0" :upgrade? false]]}
             :1.7.0 {:dependencies [[org.clojure/clojure "1.7.0" :upgrade? false]]}
             :1.8.0 {:dependencies [[org.clojure/clojure "1.8.0" :upgrade? false]]}
             :1.9.0 {:dependencies [[org.clojure/clojure "1.9.0" :upgrade? false]]}
             :dev {:resource-paths ["test-resources"],
                   :dependencies [[org.eclipse.jetty/jetty-server "9.4.19.v20190610"]
                                  [org.eclipse.jetty/jetty-security "9.4.19.v20190610"]
                                  [org.eclipse.jetty/jetty-continuation "9.4.14.v20181114"]
                                  [aleph "0.4.6"]
                                  [log4j "1.2.17"]
                                  [org.slf4j/slf4j-log4j12 "1.7.12" :upgrade? false]]}}
  :codox {:output-dir "gh-pages/doc"
          :src-dir-uri "http://github.com/cch1/http.async.client/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :release-tasks [["vcs" "assert-committed"]
                  ["v" "update"]
                  ["vcs" "push"]
                  ["deploy" "clojars"]])
