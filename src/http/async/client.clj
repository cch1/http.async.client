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

(ns http.async.client
  "Asynchronous HTTP Client - Clojure"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [await promise])
  (:require [clojure.contrib.io :as duck])
  (:use [http.async.client request headers util]
        clojure.template)
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

(do-template
 [fn-name method]
 (defn fn-name
   [#^String url & {:as options}]
   (apply execute-request
          (apply prepare-request method url (apply concat options))
          (apply concat *default-callbacks*)))
 GET :get
 POST :post
 PUT :put
 DELETE :delete
 HEAD :head
 OPTIONS :options)

(defn request-stream
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  [method #^String url body-part-callback & {:as options}]
  (apply execute-request
         (apply prepare-request method url (apply concat options))
         (apply concat (merge *default-callbacks* {:part body-part-callback}))))

(defn stream-seq
  "Creates potentially infinite lazy sequence of Http Stream."
  [method #^String url & {:as options}]
  (let [que (java.util.concurrent.LinkedBlockingQueue.)
        s-seq ((fn gen-next []
                 (lazy-seq
                  (let [v (.take que)]
                    (when-not (= ::done v)
                      (cons v (gen-next)))))))]
    (apply execute-request
           (apply prepare-request method url (apply concat options))
           (apply concat (merge
                          *default-callbacks*
                          {:part (fn [_ baos]
                                   (.put que baos)
                                   [s-seq :continue])
                           :completed (fn [_] (.put que ::done))})))))

(defn failed?
  "Checks if request failed."
  [resp]
  (delivered? (:error resp)))

(defn- safe-get
  [k r]
  (let [p (k r)]
    (if (or
         (delivered? p)
         (not (failed? r)))
      @p)))

(defn await
  "Waits for response finish."
  [response]
  (safe-get :done response))

(defn headers
  "Gets headers.
  If headers have not yet been delivered and request hasn't failed waits for headers."
  [resp]
  (safe-get :headers resp))

(defn body
  "Gets body.
  If body have not yet been delivered and request hasn't failed waits for body."
  [resp]
  (safe-get :body resp))

(defn string
  "Converts response to string."
  [resp]
  (let [enc (or (get-encoding (headers resp)) duck/*default-encoding*)
        body (body resp)
        convert (fn [#^ByteArrayOutputStream baos] (.toString baos enc))]
    (if (seq? body)
      (map convert body)
      (convert body))))

(defn cookies
  "Gets cookies from response."
  [resp]
  (create-cookies (headers resp)))

(defn done?
  "Checks if request is finished already (response receiving finished)."
  [resp]
  (delivered? (:done resp)))

(defn status
  "Gets status if status was delivered."
  [resp]
  (safe-get :status resp))
