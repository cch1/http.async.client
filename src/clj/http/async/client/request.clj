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
  (:require [http.async.client
             [status :refer :all]
             [headers :refer :all]
             [util :refer :all]
             [part :refer :all]]
            [clojure.string :refer [join]])
  (:import (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder
                                 AsyncHandler
                                 FluentCaseInsensitiveStringsMap
                                 HttpResponseStatus HttpResponseHeaders
                                 HttpResponseBodyPart
                                 PerRequestConfig
                                 Request RequestBuilder)
           (com.ning.http.client.cookie Cookie)
           (java.net URLEncoder)
           (java.io File
                    InputStream
                    ByteArrayInputStream
                    ByteArrayOutputStream)))

(def ^:dynamic *user-agent* "http.async.client")

(def convert-method
  ^{:doc   "Converts clj method (:get, :put, . ..) to Async Client specific."}
  {:get     "GET"
   :post    "POST"
   :put     "PUT"
   :delete  "DELETE"
   :head    "HEAD"
   :options "OPTIONS"
   :patch   "PATCH"})

(defn get-encoding
  "Gets content encoding from headers, if Content-Type header not present
  or media-type in it is missing => nil"
  [{ct :content-type
    :or {ct ""}}]
  (when-let [match (re-matches #".*charset\s*=\s*(.*)\s*" ct)]
    (.toUpperCase (match 1))))

;; default set of callbacks

;; status callbacks
(defn status-collect
  "Returns all status and procides with execution"
  [_ status]
  [status :continue])

;; header callbacks
(defn headers-collect
  "Reurns all headers, or aborts if no headers provided."
  [_ headers]
  [headers (if-not headers :abort)])

;; body callbacks
(defn body-collect [state baos]
  (let [body (:body state)]
    (if (realized? body)
      (do
        (.writeTo baos @body)
        [@body :continue])
      [baos :continue])))

;; completed callbacks
(defn body-completed [_] [true :continue])

;; error callbacks
(defn error-collect [_ t] t)

;; default set of callbacks
(def
  ^{:doc "Default set of callbacks."
    :dynamic true}
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
    (URLEncoder/encode (join \& (map #(join \= (map url-encode %)) arg)) "UTF-8")
    (URLEncoder/encode (name arg) "UTF-8")))

(defn prepare-request
  "Prepares method (GET, POST, ..) request to url.
  Options:
    :query   - map of query parameters, if value is vector than multiple values
               will be send as n=v1&n=v2
    :headers - map of headers
    :body    - body
    :cookies - cookies to send
    :proxy   - map with proxy configuration to be used
      :host     - proxy host
      :port     - proxy port
      :protocol - (optional) protocol to communicate with proxy,
                  :http (default, if you provide no value) and :https are allowed
      :user     - (optional) user name to use for proxy authentication,
                  has to be provided with :password
      :password - (optional) password to use for proxy authentication,
                  has to be provided with :user
    :auth    - map with authentication to be used
      :type       - either :basic or :digest
      :user       - user name to be used
      :password   - password to be used
      :realm      - realm name to authenticate in
      :preemptive - assume authentication is required
    :timeout - request timeout in ms"
  {:tag Request}
  [method #^String url & {:keys [headers
                                 query
                                 body
                                 cookies
                                 proxy
                                 auth
                                 timeout]}]
  (let [rb (RequestBuilder. #^String (convert-method method))]
    ;; headers
    (doseq [[k v] headers] (.addHeader rb (name k) (str v)))
    ;; cookies
    (doseq [{:keys [domain name value path expires max-age secure http-only?]
             :or {path "/" max-age 30 secure false expires 0 http-only? false}} cookies]
      (.addCookie rb (Cookie/newValidCookie name value domain value path
                                            expires max-age secure http-only?)))
    ;; query parameters
    (doseq [[k v] query] (if (vector? v)
                           (doseq [vv v] (.addQueryParameter rb (name k) (str vv)))
                           (.addQueryParameter rb (name k)(str v))))
    ;; message body
    (cond
      (map? body) (doseq [[k v] body] (.addParameter rb (name k) (str v)))
      (string? body) (.setBody rb (.getBytes (if (= "application/x-www-form-urlencoded" (:content-type headers))
                                               (url-encode body)
                                               body)
                                             "UTF-8"))
      (instance? InputStream body) (.setBody rb #^InputStream body)
      (instance? File body) (.setBody rb #^File body)
      (vector? body) (doseq [part body]
                       ;; each part should be map with all details
                       ;; needed to create body part
                       (.addBodyPart rb (create-part part))))
    (when auth
      (set-realm auth rb))
    (when proxy
      (set-proxy proxy rb))
    ;; request timeout
    (when timeout
      (let [prc (PerRequestConfig.)]
        (.setRequestTimeoutInMs prc timeout)
        (.setPerRequestConfig rb prc)))
    (.. rb (setUrl url) (build))))

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
  - :status    - status callback (optional, defaults to status-collect)
  - :headers   - headers callback (optional, defaults to headers-collect)
  - :part      - body part callback (optional, defaults to body-collect)
  - :completed - response completed (optional, defaults to body-completed)
  - :error     - error callback (optional, defaults to error-collect)

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
  [client #^Request req & {status    :status
                           headers   :headers
                           part      :part
                           completed :completed
                           error     :error}]
  (let [resp {:id      (gensym "req-id__")
              :req req
              :url     (.getUrl req)
              :status  (promise)
              :headers (promise)
              :body    (promise)
              :done    (promise)
              :error   (promise)}
        resp-future
        (.executeRequest
         client req
         (reify AsyncHandler
           (^{:tag com.ning.http.client.AsyncHandler$STATE}
            onStatusReceived [this #^HttpResponseStatus e]
            (let [[result action] ((or status
                                       (:status *default-callbacks*))
                                   resp (convert-status-to-map e))]
              (deliver (:status resp) result)
              (convert-action action)))
           (^{:tag com.ning.http.client.AsyncHandler$STATE}
            onHeadersReceived [this #^HttpResponseHeaders e]
            (let [[result action] ((or headers
                                       (:headers *default-callbacks*))
                                   resp (convert-headers-to-map e))]
              (deliver (:headers resp) result)
              (convert-action action)))
           (^{:tag com.ning.http.client.AsyncHandler$STATE}
            onBodyPartReceived [this #^HttpResponseBodyPart e]
            (let [l (.length e)]
              (when (pos? l)
                ;; TODO: use native getBodyByteBuffer() instead of alloc'ing our own here
                (let [baos (ByteArrayOutputStream. l)]
                  (.write baos (.getBodyPartBytes e) 0 l)
                  (let [[result action] ((or part
                                             (:part *default-callbacks*))
                                         resp baos)
                        body (:body resp)]
                    (when-not (realized? body)
                      (deliver body result))
                    (convert-action action))))))
           (^{:tag Object}
            onCompleted [this]
            (do
              ((or completed
                   (:completed *default-callbacks*))
               resp)
              (when-not (realized? (:body resp))
                (deliver (:body resp) nil))
              (deliver (:done resp) true)))
           (^{:tag void}
            onThrowable [this #^Throwable t]
            (do
              (deliver (:error resp) (try
                                       ((or error
                                            (:error *default-callbacks*))
                                        resp t)
                                       (catch Throwable e
                                         e)))
              (when-not (realized? (:done resp))
                (deliver (:done resp) true))))))]
    (with-meta resp {:started (System/currentTimeMillis)
                     :cancelled? (fn [] (.isCancelled resp-future))
                     :cancel (fn [] (.cancel resp-future true))})))
