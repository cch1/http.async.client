(defproject ahc-clj "0.1.0"
  :description      "Async Http Client for Clojure"
  :namespaces       [async.http.client]
  :dependencies     [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
		     [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
		     [com.ning/async-http-client "1.0.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [leiningen/lein-swank "1.1.0"]
		     [autodoc "0.7.1"]
		     [leiningen/lein-javac "1.0.1"]
                     [org.eclipse.jetty/jetty-server "7.1.4.v20100610"]]
  :autodoc {:name "ahc-clj"
            :web-src-dir "http://github.com/neotyk/ahc-clj/blob/"
            :web-home "http://neotyk.github.com/ahc-clj/autodoc/"
            :copyright "Copyright 2010 Hubert Iwaniuk"})
