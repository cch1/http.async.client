(defproject http.async "0.1.0"
  :description      "Async Http Client for Clojure"
  :namespaces       [async.http.client]
  :dependencies     [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
		     [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
		     [com.ning/async-http-client "1.0.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [leiningen/lein-swank "1.1.0"]
		     [autodoc "0.7.0"]
		     [leiningen/lein-javac "1.0.0"]
                     [org.eclipse.jetty/jetty-server "7.1.4.v20100610"]])
