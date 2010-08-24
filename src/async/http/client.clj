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

(ns async.http.client
  "Async HTTP Client - Clojure"
  {:author "Hubert Iwaniuk"}
  (:require [clojure.contrib.io :as duck])
  (:use async.http.client.request
        async.http.client.headers)
  (:import (java.io ByteArrayOutputStream)
           (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder)))

(defn create-client
  "Creates new Async Http Client"
  [& {user-agent :user-agent}]
  (AsyncHttpClient.
   (.build
    (.setUserAgent (AsyncHttpClientConfig$Builder.)
                   (if user-agent
                     user-agent
                     ahc-user-agent)))))

(defmacro with-ahc
  "Creates new Async Http Client with given configuration
  than executes body and closes the client."
  [config & body]
  `(with-open [c# (create-client ~@(apply concat config))]
     (binding [*ahc* c#]
       ~@body)))

(defn GET
  "GET resource from url. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :get url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn POST
  "POST to resource. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :post url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn PUT
  "PUT to resource. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :put url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn DELETE
  "DELETE resource from url. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :delete url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn HEAD
  "Request HEAD from url. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :head url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn OPTIONS
  "Request OPTIONS from url. Returns promise, that is delivered once response is completed."
  [#^String url & {:as options}]
  (execute-request (apply prepare-request :options url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-collect
                   :completed body-completed
                   :error error-collect))

(defn request-stream
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  [method #^String url body-part-callback & {:as options}]
  (execute-request (apply prepare-request method url (apply concat options))
                   :status status-collect
                   :headers headers-collect
                   :part body-part-callback
                   :completed body-completed
                   :error error-collect))

(defn stream-seq
  "Creates potentially infinite lazy sequence of Http Stream."
  [method #^String url & {:as options}]
  (let [que (java.util.concurrent.LinkedBlockingQueue.)
        s-seq ((fn gen-next []
                 (lazy-seq
                  (let [v (.take que)]
                    (when-not (= ::done v)
                      (cons v (gen-next)))))))]
    (consume-stream (apply prepare-request method url (apply concat options))
                    :status status-collect
                    :headers headers-collect
                    :part (fn [state baos]
                            (.put que baos)
                            (if-not (contains? @state :body)
                              (dosync (alter state assoc :body s-seq))))
                    :completed (fn [state]
                                 (.put que ::done))
                    :error error-collect)))

(defn string
  "Converts response to string."
  [resp]
  (let [enc (or (get-encoding (:headers @resp)) duck/*default-encoding*)
        body (:body @resp)
        convert (fn [#^ByteArrayOutputStream baos] (.toString baos enc))]
    (if (seq? body)
      (map convert body)
      (convert body))))

(defn cookies
  "Gets cookies from response."
  [resp]
  (if-let [headers-received (:headers-received @resp)]
    (@headers-received))
  (if-let [cookies (:cookies @resp)]
    cookies
    (let [cookies (create-cookies (:headers @resp))]
      (if (instance? clojure.lang.Ref resp)
        (dosync (alter resp assoc :cookies cookies))
        (assoc @resp :cookies cookies))
      cookies)))
