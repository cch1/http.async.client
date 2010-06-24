(ns async.http.client.test
  "Testing of ahc-clj"
  #^{:author "Hubert Iwaniuk <neotyk@kungfoo.pl>"}
  (:use clojure.test async.http.client))

(deftest test-status
  (let [code (promise)]
    (execute-request
     (prepare-get "http://localhost:8080/")
     body-collect body-completed ignore-headers
     (fn [_ st] (do (println st) (deliver code (:code st)) :abort)))
    (is (= @code 200))))