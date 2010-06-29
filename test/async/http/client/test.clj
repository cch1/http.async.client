; Copyright 2010 Hubert Iwaniuk
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;   http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns async.http.client.test
  "Testing of ahc-clj"
  #^{:author "Hubert Iwaniuk <neotyk@kungfoo.pl>"}
  (:use clojure.test async.http.client)
  (:import (org.apache.log4j ConsoleAppender Level Logger PatternLayout)))

(defn once-fixture [f]
  "Configures Logger before test here are executed, and closes AHC after tests are done."
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
    (is (= (status :msg) "OK"))
    (is (= (status :protocol) "HTTP/1.1"))
    (is (= (status :major) 1))
    (is (= (status :minor) 1))))

(deftest test-headers
  (let [headers# (promise)
        _ (execute-request
           (prepare-get "http://localhost:8080/")
           body-collect body-completed
           (fn [_ hds] (do (deliver headers# hds) :abort)))
        headers @headers#]
    (println headers)
    (is (= (headers :server) "Jetty"))))

(deftest test-body
  (let [resp (execute-request
              (prepare-get "http://localhost:8080/"))]
    (println (apply str (map #(char %) (:body @resp))))
    (is (not (empty? (:body @resp))))
    (if (contains? (:headers @resp) :content-length)
      (is (= (count (:body @resp)) (:content-length (:headers @resp)))))))
