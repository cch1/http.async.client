(defproject http.async "0.1.0"
  :description      "Async Http Client for Clojure"
  :namespaces       [async.http.client]
  :source-path "src/clj"
  :java-source-path "src/java"
  :javac-fork "true"
  :dependencies     [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
		     [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
		     [com.ning/async-http-client "1.0.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [leiningen/lein-swank "1.1.0"]
		     [autodoc "0.7.0"]
		     [lein-javac "0.0.2-SNAPSHOT"]])
