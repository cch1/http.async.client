(defproject http.async.client "0.5.3-SNAPSHOT"
  :name             "http.async.client"
  :description      "Asynchronous HTTP Client for Clojure"
  :url              "http://neotyk.github.com/http.async.client/"
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.ning/async-http-client "1.7.10" :exclusions [org.slf4j/slf4j-api]]]
  :min-lein-version "2.0.0"
  :plugins [[codox "0.8.12"]
            [lein-difftest "2.0.0"]]
  :profiles {:1.4.0 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5.1 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :dev {:resource-paths ["test-resources"],
                   :dependencies [[org.eclipse.jetty/jetty-server "9.3.0.RC0"]
                                  [org.eclipse.jetty/jetty-security "9.3.0.RC0"]
                                  [org.eclipse.jetty/jetty-continuation "9.3.0.RC0"]
                                  [log4j "1.2.17"]
                                  [org.slf4j/slf4j-log4j12 "1.7.12"]]}}
  ;; :repositories {"snapshots" "http://oss.sonatype.org/content/repositories/snapshots/"}
  :codox {:output-dir "doc"}
  :autodoc {:web-src-dir "http://github.com/neotyk/http.async.client/blob/"
            :web-home "http://neotyk.github.com/http.async.client/autodoc/"
            :copyright "Copyright 2012 Hubert Iwaniuk"}
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo})
