(ns sample
  (:require [async.http.client :as c]))

(let [r (c/GET "http://example.com/")]
  (println "done?" (c/done? r))
  (c/await r)
  (println "done?" (c/done? r))
  (c/string r))
