(ns twitter-sample
  (:require [http.async.client :as c]
            [org.danlarkin.json :as j]))

;; Please consider using https://github.com/adamwynne/twitter-api

(def u "username")
(def p "password")

(defn print-user-and-text [prefix s]
  (let [twit (j/decode-from-str s)
        user (:screen_name (:user twit))
        text (:text twit)]
    (println prefix ":" user "=>" text)))

;; statuses/sample
(defn statuses-sample []
  (with-open [client (c/create-client)]
    (doseq [twit-str (c/string
                      (c/stream-seq client :get "http://stream.twitter.com/1/statuses/sample.json"
                                    :auth {:user u :password p}))]
      (print-user-and-text "sample" twit-str))))

;; statuses/filter
(defn statuses-filter []
  (with-open [client (c/create-client)]
    (doseq [twit-str (c/string
                      (c/stream-seq client :post "http://stream.twitter.com/1/statuses/filter.json"
                                    :body {"track" "basketball,football,baseball,footy,soccer"}
                                    :auth {:user u :password p}))]
      (print-user-and-text "sports" twit-str))))
