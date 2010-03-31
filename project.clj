(defproject http.async "0.1.0"
  :description      "Async Http Client for Clojure"
  :namespaces       [http.async]
  :source-path "src/clj"
  :java-source-path "src/java"
  :javac-fork "true"
  :dependencies     [[org.clojure/clojure "1.1.0"]
		     [org.clojure/clojure-contrib "1.1.0"]
		     [com.ning/async-http-client "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[leiningen/lein-swank "1.1.0"]
		     [autodoc "0.7.0"]
		     [lein-javac "0.0.2-SNAPSHOT"]])

