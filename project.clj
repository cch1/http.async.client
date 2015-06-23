(defproject http.async.client "0.6.0"
  :name             "http.async.client"
  :description      "Asynchronous HTTP Client for Clojure"
  :url              "https://github.com/cch1/http.async.client"
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.ning/async-http-client "1.8.16"]]
  :min-lein-version "2.5.1"
  :plugins [[codox "0.8.12"]
            [lein-difftest "2.0.0"]]
  :profiles {:1.3.0 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4.0 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5.1 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :dev {:resource-paths ["test-resources"],
                   :dependencies [[org.eclipse.jetty/jetty-server "9.3.0.RC1"]
                                  [org.eclipse.jetty/jetty-security "9.3.0.RC1"]
                                  [org.eclipse.jetty/jetty-continuation "9.3.0.RC1"]
                                  [log4j "1.2.17"]
                                  [org.slf4j/slf4j-log4j12 "1.7.12"]]}}
  ;; :repositories {"snapshots" "http://oss.sonatype.org/content/repositories/snapshots/"}
  :codox {:output-dir "gh-pages/doc"
          :src-dir-uri "http://github.com/cch1/http.async.client/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo})
