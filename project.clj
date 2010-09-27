(defproject http.async.client "0.2.1-SNAPSHOT"
  :description      "Asynchronous HTTP Client for Clojure"
  :namespaces       [http.async.client]
  :dependencies     [[org.clojure/clojure "1.2.0"]
		     [org.clojure/clojure-contrib "1.2.0"]
		     [com.ning/async-http-client "1.2.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [autodoc "0.7.1"]
		     [org.clojars.neotyk/lein-javac "1.0.2"]
                     [lein-difftest "1.2.2"]
                     [org.eclipse.jetty/jetty-server "7.1.4.v20100610"]
                     [org.danlarkin/clojure-json "1.1"]
                     [lein-run "1.0.0"]]
  :repository {"snapshots" "http://oss.sonatype.org/content/repositories/snapshots/"}
  :hooks [leiningen.hooks.javac]
  :autodoc {:name "http.async.client"
            :web-src-dir "http://github.com/neotyk/http.async.client/blob/"
            :web-home "http://neotyk.github.com/http.async.client/autodoc/"
            :copyright "Copyright 2010 Hubert Iwaniuk"})
