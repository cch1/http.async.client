(defproject http.async.client "0.5.0-SNAPSHOT"
  :name             "http.async.client"
  :description      "Asynchronous HTTP Client for Clojure"
  :url              "http://neotyk.github.com/http.async.client/"
  :source-path      "src/clj"
  :java-source-path "src/jvm"
  :javac-options {:deprecation "true"}
  :dependencies     [[org.clojure/clojure "1.4.0"]
		     [com.ning/async-http-client "1.7.5"]]
  :dev-dependencies [[org.eclipse.jetty/jetty-server "7.1.4.v20100610"]
                     [org.eclipse.jetty/jetty-security "7.1.4.v20100610"]
                     [lein-difftest "1.3.3"  :exclusions [org.clojure/clojure
                                                          org.clojure/clojure-contrib]]
                     [log4j "1.2.13"]
                     [org.slf4j/slf4j-log4j12 "1.6.4"]
                     [codox "0.6.1"]]
  ;; :repositories {"snapshots" "http://oss.sonatype.org/content/repositories/snapshots/"}
  :codox {:output-dir "doc"}
  :autodoc {:web-src-dir "http://github.com/neotyk/http.async.client/blob/"
            :web-home "http://neotyk.github.com/http.async.client/autodoc/"
            :copyright "Copyright 2012 Hubert Iwaniuk"}
  :licence {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo})
