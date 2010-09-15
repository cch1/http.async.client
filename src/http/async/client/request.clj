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

(ns http.async.client.request
  "Asynchronous HTTP Client - Clojure - Requesting API"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [promise])
  (:require [clojure.contrib.io :as duck])
  (:use [http.async.client status headers util]
        [clojure.stacktrace]
        [clojure.contrib.java-utils :only [as-str]]
        [clojure.contrib.str-utils :only [str-join]])
  (:import (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder
                                 AsyncHandler Cookie
                                 FluentCaseInsensitiveStringsMap
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart
                                 PerRequestConfig
                                 Realm$RealmBuilder Realm$AuthScheme
                                 Request RequestBuilder
				 RequestType ProxyServer)
           (ahc RequestBuilderWrapper)
           (java.net URLEncoder)
           (java.io InputStream
                    ByteArrayInputStream
                    ByteArrayOutputStream)))

(def ahc-user-agent "ahc-clj/0.2.0-dev")

(def *ahc*
     (AsyncHttpClient.
      (.build
       (.setUserAgent (AsyncHttpClientConfig$Builder.) ahc-user-agent))))

(defn- convert-method [method]
  "Converts clj method (:get, :put, ...) to Async Client specific.
   Defaults to :get"
  (cond
   (= method :get) RequestType/GET
   (= method :post) RequestType/POST
   (= method :put) RequestType/PUT
   (= method :delete) RequestType/DELETE
   (= method :head) RequestType/HEAD
   (= method :options) RequestType/OPTIONS
   :default RequestType/GET))

(defn get-encoding
  "Gets content encoding from headers, if Content-Type header not present
  or media-type in it is missing => nil"
  [{ct :content-type}]
  (when-let [match (re-matches #".*charset\s*=\s*(.*)\s*" ct)]
    (.toUpperCase (match 1))))

;; default set of callbacks

;; status callbacks
(defn status-collect [_ status]
  "Returns all status and procides with execution"
  [status :continue])

;; header callbacks
(defn headers-collect [_ headers]
  "Reurns all headers, or aborts if no headers provided."
  [headers (if-not headers :abort)])

;; body callbacks
(defn body-collect [state baos]
  (let [body (:body state)]
    (if (delivered? body)
      (do
        (.writeTo baos @body)
        [@body :continue])
      [baos :continue])))

;; completed callbacks
(defn body-completed [state]
  [true :continue])

;; error callbacks
(defn error-collect [state t]
  t)

;; default set of callbacks
(def
 ^{:doc "Default set of callbacks."}
 *default-callbacks*
 {:status status-collect
  :headers headers-collect
  :part body-collect
  :completed body-completed
  :error error-collect})

(defn url-encode
  "Taken from Clojure Http Client"
  [arg]
  (if (map? arg)
    (URLEncoder/encode (str-join \& (map #(str-join \= (map url-encode %)) arg)) "UTF-8")
    (URLEncoder/encode (as-str arg) "UTF-8")))

(defn prepare-request
  "Prepares method (GET, POST, ..) request to url.
  Options:
    :query   - map of query parameters
    :headers - map of headers
    :body    - body
    :cookies - cookies to send
    :proxy   - map with proxy configuration to be used (:host and :port)
    :auth    - map with authentication to be used
      :type     - either :basic or :digest
      :user     - user name to be used
      :password - password to be used
      :realm    - realm name to authenticate in
    :timeout - request timeout in ms"
  {:tag Request}
  [method #^String url & {headers :headers
                          query :query
                          body :body
                          cookies :cookies
                          proxy :proxy
                          auth :auth
                          timeout :timeout}]
  ;; RequestBuilderWrapper is needed for now, until RequestBuilder
  ;; is able to be used directly from Clojure.
  (let [#^RequestBuilderWrapper rbw
        (RequestBuilderWrapper.
         (RequestBuilder. (convert-method method)))]
    ;; headers
    (doseq [[k v] headers] (.addHeader rbw
                                       (if (keyword? k) (name k) k)
                                       (str v)))
    ;; cookies
    (doseq [{domain :domain
             name :name
             value :value
             path :path
             max-age :max-age
             secure :secure
             :or {path "/"
                  max-age 30
                  secure false}} cookies]
      (.addCookie rbw (Cookie. domain name value path max-age secure)))
    ;; query parameters
    (doseq [[k v] query] (.addQueryParameter rbw
                                             (if (keyword? k) (name k) k)
                                             (str v)))
    ;; message body
    (cond
     (map? body) (doseq [[k v] body]
                   (.addParameter rbw
                                  (if (keyword? k) (name k) k)
                                  (str v)))
     (string? body) (.setBody rbw (.getBytes (url-encode body) "UTF-8"))
     (instance? InputStream body) (.setBody rbw body))
    ;; authentication
    (when-let [{type :type
                user :user
                password :password
                realm :realm
                :or {:type :basic}} auth]
      (let [rbld (Realm$RealmBuilder.)]
        (if (nil? user)
          (if (nil? password)
            (throw (IllegalArgumentException. "For authentication user and password is required"))
            (throw (IllegalArgumentException. "For authentication user is required"))))
        (if (nil? password)
          (throw (IllegalArgumentException. "For authentication password is required")))
        (if (= :digest type)
          (do
            (if (nil? realm) (throw (IllegalArgumentException.
                                     "For DIGEST authentication realm is required")))
            (.setRealmName rbld realm)
            (.setScheme rbld Realm$AuthScheme/DIGEST)))
        (doto rbld
          (.setPrincipal user)
          (.setPassword password))
        (.setRealm rbw (.build rbld))))
    ;; proxy
    (if proxy
      (.setProxyServer rbw (ProxyServer. (:host proxy) (:port proxy))))
    ;; request timeout
    (if timeout
      (let [prc (PerRequestConfig.)]
        (.setRequestTimeoutInMs prc timeout)
        (.setPerRequestConfig rbw prc)))
    ;; fine
    (.. (.getRequestBuilder rbw) (setUrl url) (build))))

(defn convert-action
  "Converts action (:abort, nil) to Async client STATE."
  [action]
  {:tag com.ning.http.client.AsyncHandler$STATE}
  (if (= action :abort)
    com.ning.http.client.AsyncHandler$STATE/ABORT
    com.ning.http.client.AsyncHandler$STATE/CONTINUE))

(defn execute-request
  "Executes provided request.
  Arguments:
  - req        - request to be executed
  - :status    - status callback
  - :headers   - headers callback
  - :part      - body part callback
  - :completed - response completed
  - :error     - error callback

  Returns a map:
  - :id      - unique ID of request
  - :status  - promise that once status is received is delivered, contains lazy map of:
    - :code     - response code
    - :msg      - response message
    - :protocol - protocol with version
    - :major    - major version of protocol
    - :minor    - minor version of protocol
  - :headers - promise that once headers are received is delivered, contains lazy map of:
    - :server - header names are keyworded, values stay not changed
  - :body    - body of response, depends on request type, might be ByteArrayOutputStream
               or lazy sequence, use conveniece methods to extract it, like string
  - :done    - promise that is delivered once receiving response has finished
  - :error   - promise that is delivered if requesting resource failed, once delivered
               will contain Throwable."
  [#^Request req & {status    :status
                    headers   :headers
                    part      :part
                    completed :completed
                    error     :error}]
  (let [resp {:id      (gensym "req-id__")
              :status  (promise)
              :headers (promise)
              :body    (promise)
              :done    (promise)
              :error   (promise)}
        resp-future
        (.executeRequest
         *ahc* req
         (proxy [AsyncHandler] []
           (onStatusReceived [#^HttpResponseStatus e]
                             (let [[result action] (status resp (convert-status-to-map e))]
                               (deliver (:status resp) result)
                               (convert-action action)))
           (onHeadersReceived [#^HttpResponseHeaders e]
                              (let [[result action] (headers resp (convert-headers-to-map e))]
                                (deliver (:headers resp) result)
                                (convert-action action)))
           (onBodyPartReceived [#^HttpResponseBodyPart e]
                               (when-let [bytes (.getBodyPartBytes e)]
                                 (let [length (alength bytes)
                                       baos (ByteArrayOutputStream. length)]
                                   (.write baos bytes 0 length)
                                   (let [[result action] (part resp baos)
                                         body (:body resp)]
                                     (if-not (delivered? body)
                                       (deliver body result))
                                     (convert-action action)))))
           (onCompleted []
                        (do
                          (completed resp)
                          (deliver (:done resp) true)))
           (onThrowable [#^Throwable t]
                        (do
                          (deliver (:error resp) (error resp t))
                          (deliver (:done resp) true)))))]
    (with-meta resp {:started (System/currentTimeMillis)
                     :cancelled? (fn [] (.isCancelled resp-future))
                     :cancel (fn [] (.cancel resp-future true))})))
