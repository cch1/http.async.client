(defproject http.async.client "0.4.4-SNAPSHOT"
  :name             "http.async.client"
  :description      "Asynchronous HTTP Client for Clojure"
  :url              "http://neotyk.github.com/http.async.client/"
  :source-path      "src/clj"
  :java-source-path "src/jvm"
  :javac-options {:deprecation "true"}
  :dependencies     [[org.clojure/clojure "1.3.0"]
		     [com.ning/async-http-client "1.7.1"]]
  :plugins [[codox "0.6.1"]]
  :dev-dependencies [[org.eclipse.jetty/jetty-server "7.1.4.v20100610"]
                     [org.eclipse.jetty/jetty-security "7.1.4.v20100610"]
                     [lein-difftest "1.3.3"  :exclusions [org.clojure/clojure
                                                          org.clojure/clojure-contrib]]
                     [log4j "1.2.13"]
                     [org.slf4j/slf4j-log4j12 "1.6.4"]]
  ;; :repositories {"snapshots" "http://oss.sonatype.org/content/repositories/snapshots/"}
  :codox {:output-dir "doc"}
  :autodoc {:web-src-dir "http://github.com/neotyk/http.async.client/blob/"
            :web-home "http://neotyk.github.com/http.async.client/autodoc/"
            :copyright "Copyright 2012 Hubert Iwaniuk"})
