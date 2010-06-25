(ns async.http.client.test
  "Testing of ahc-clj"
  #^{:author "Hubert Iwaniuk <neotyk@kungfoo.pl>"}
  (:use clojure.test async.http.client)
  (:import (org.apache.log4j ConsoleAppender Level Logger PatternLayout)))

(defn once-fixture [f]
  (doto (Logger/getRootLogger)
    (.setLevel Level/WARN)
    (.addAppender (ConsoleAppender. (PatternLayout. PatternLayout/TTCC_CONVERSION_PATTERN))))
  (f)
  (.close ahc))

(use-fixtures :once once-fixture)

(deftest test-status
  (let [status# (promise)
	_ (execute-request
	   (prepare-get "http://localhost:8080/")
	   body-collect body-completed ignore-headers
	   (fn [_ st] (do (deliver status# st) :abort)))
	status @status#]
    (is (= (status :code) 200))
    (is (= (status :text) "OK"))
    (is (= (status :protocol) "HTTP/1.1"))
    (is (= (status :major) 1))
    (is (= (status :minor) 1))))