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

(ns
    #^{:author "Hubert Iwaniuk"
       :doc "Async HTTP Client - Clojure"}
  async.http.client
  (:use async.http.client.request))

(defn GET
  "GET resource from url. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (GET url {}))
  ([#^String url options]
     (execute-request (prepare-request :get url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn POST
  "POST to resource. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (POST url []))
  ([#^String url body]
     (POST url body {}))
  ([#^String url body options]
     (execute-request (prepare-request :post url options body)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn PUT
  "PUT to resource. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (PUT url []))
  ([#^String url body]
     (PUT url body {}))
  ([#^String url body options]
     (execute-request (prepare-request :put url options body)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn DELETE
  "DELETE resource from url. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (DELETE url {}))
  ([#^String url options]
     (execute-request (prepare-request :delete url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn HEAD
  "Request HEAD from url. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (HEAD url {}))
  ([#^String url options]
     (execute-request (prepare-request :head url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn OPTIONS
  "Request OPTIONS from url. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (OPTIONS url {}))
  ([#^String url options]
     (execute-request (prepare-request :options url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn STREAM
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  ([method #^String url body-part-callback]
     (STREAM method url body-part-callback {}))
  ([method #^String url body-part-callback options]
     (execute-request (prepare-request method url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-part-callback
                       :completed body-completed
                       :error error-collect})))

(defn STREAM-SEQ
  "Creates potentially infinite lazy sequence of Http Stream."
  ([method #^String url & {:as options :or {}}]
     (let [que (java.util.concurrent.LinkedBlockingQueue.)
           s-seq ((fn thisfn []
                    (lazy-seq
                     (let [v (.take que)]
                       (when-not (= ::done v)
                         (cons v (thisfn)))))))]
       (consume-stream (prepare-request method url options)
                       :status status-collect
                       :headers headers-collect
                       :part (fn [state bytes]
                               (when-not (empty? bytes)
                                 (let [v (apply str (map char bytes))]
                                   (.put que v)
                                   (if-not (contains? @state :body )
                                     (dosync (alter state assoc :body s-seq))))))
                       :completed (fn [state]
                                    (.put que ::done))
                       :error error-collect))))
