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

(ns async.http.client.request
  "Async HTTP Client - Clojure - Requesting API"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [promise])
  (:require [clojure.contrib.io :as duck])
  (:use [async.http.client status headers util]
        [clojure.stacktrace]
        [clojure.contrib.java-utils :only [as-str]]
        [clojure.contrib.str-utils :only [str-join]])
  (:import (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder
                                 AsyncHandler Cookie
                                 FluentCaseInsensitiveStringsMap
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart Request RequestBuilder
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
(defn accept-ok [_ status]
  (if (not (= (:code status) 200)) :abort))

(defn status-collect [state status]
  "Stores status map under :status in state."
  (dosync (alter state assoc :status status)))

(defn status-print [state st]
  (println (str (:id @state) "< " (:protocol st) " " (:code st) " " (:msg st))))

(defn new-status-collect [_ status]
  "Returns all status and procides with execution"
  [status :continue])

;; header callbacks
(defn headers-collect [state headers]
  "Stores headers under :headers in state."
  (if headers
    (dosync (alter state assoc :headers headers))
    (do
      (println "Received empty headers, aborting.")
      :abort)))

(defn print-headers [state headers]
  (doall (map #(println (str (:id @state) "< " % ": " (get headers %))) (keys headers))))

(defn new-headers-collect [_ headers]
  "Reurns all headers, or aborts if no headers provided."
  [headers (if-not headers :abort)])

;; body callbacks
(defn body-collect [state baos]
  "Stores body parts under :body in state."
  (if (:body @state)
    (.writeTo baos (:body @state))
    (dosync (alter state assoc :body baos))))

(defn new-body-collect [state baos]
  (let [body (:body state)]
    (if (delivered? body)
      (do
        (.writeTo baos @body)
        [@body :continue])
      [baos :continue])))

;; completed callbacks
(defn body-completed [state]
  "Provides value that will be delivered to response promise."
  @state)

(defn new-body-completed [state]
  [true :continue])

;; error callbacks
(defn error-collect [state t]
  "Stores exception under :error in state"
  (dosync (alter state assoc :error t)))

(defn new-error-collect [state t]
  [t :continue])

;; default set of callbacks
(def *default-callbacks*
     {:status new-status-collect
      :headers new-headers-collect
      :part new-body-collect
      :completed new-body-completed
      :error new-error-collect})

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
    :proxy   - map with proxy configuration to be used (:host and :port)"
  {:tag Request}
  [method #^String url & {headers :headers
                          query :query
                          body :body
                          cookies :cookies
                          proxy :proxy}]
  ;; RequestBuilderWrapper is needed for now, until RequestBuilder
  ;; is able to be used directly from Clojure.
  (let [#^RequestBuilderWrapper rbw
        (RequestBuilderWrapper.
         (RequestBuilder. (convert-method method)))]
    (doseq [[k v] headers] (.addHeader rbw
                                       (if (keyword? k) (name k) k)
                                       (str v)))
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
    (doseq [[k v] query] (.addQueryParameter rbw
                                             (if (keyword? k) (name k) k)
                                             (str v)))
    (cond
     (map? body) (doseq [[k v] body]
                   (.addParameter rbw
                                  (if (keyword? k) (name k) k)
                                  (str v)))
     (string? body) (.setBody rbw (.getBytes (url-encode body) "UTF-8"))
     (instance? InputStream body) (.setBody rbw body))
    (if proxy
      (.setProxyServer rbw (ProxyServer. (:host proxy) (:port proxy))))
    (.. (.getRequestBuilder rbw) (setUrl url) (build))))

(defn convert-action
  "Converts action (:abort, nil) to Async client STATE."
  [action]
  {:tag com.ning.http.client.AsyncHandler$STATE}
  (if (= action :abort)
    com.ning.http.client.AsyncHandler$STATE/ABORT
    com.ning.http.client.AsyncHandler$STATE/CONTINUE))

(defn consume-stream
  "Executes provided request, assuming target will stream..
  Arguments:
  - req - prepared request
  - options
    - :status - status callback
    - :headers - headers callback
    - :part - body part callback
    - :completed - request completed
    - :error - error callback"
  [#^Request req & {status-fn    :status
                    headers-fn   :headers
                    part-fn      :part
                    completed-fn :completed
                    error-fn     :error}]
  (let [resp (ref {:id (gensym "req-id__")
                   :status-received (promise)
                   :headers-received (promise)
                   :body-started (promise)
                   :body-finished (promise)
                   :errored (promise)})
        body-started (ref false)]
    (.executeRequest
     *ahc* req
     (proxy [AsyncHandler] []
       (onStatusReceived [#^HttpResponseStatus e]
                         (let [action (status-fn resp (convert-status-to-map e))]
                           (deliver (:status-received @resp) true)
                           (convert-action action)))
       (onHeadersReceived [#^HttpResponseHeaders e]
                          (let [action (headers-fn resp (convert-headers-to-map e))]
                            (deliver (:headers-received @resp) true)
                            (convert-action action)))
       (onBodyPartReceived  [#^HttpResponseBodyPart e]
                            (when-let [bytes (.getBodyPartBytes e)]
                              (let [baos (ByteArrayOutputStream. (alength bytes))]
                                (.write baos bytes 0 (alength bytes))
                                (let [action (part-fn resp baos)]
                                  (when-not @body-started
                                    (dosync (alter body-started (fn [_ a] a) true)
                                            (deliver (:body-started @resp) true)))
                                  (convert-action action)))))
       (onCompleted [] (do
                         (deliver (:body-finished @resp) true)
                         (completed-fn resp)))
       (onThrowable [#^Throwable t]
                    (do
                      (print-cause-trace t)
                      (deliver (:errored @resp) true)
                      (error-fn resp t)))))
    resp))

(defn execute-request
  "Executes provided request.
  Arguments:
  - req        - request to be executed
  - :status    - status callback
  - :headers   - headers callback
  - :part      - body part callback
  - :completed - response completed
  - :error     - error callback"
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
              :error   (promise)}]
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
                      (print-cause-trace t)
                      (deliver (:error resp) (error resp t))))))
    ^{:started (System/currentTimeMillis)}
    resp))

(defn failed?
  "Checks if request failed."
  [resp]
  (delivered? (:error resp)))

(defn status
  "Gets status if status was delivered."
  [resp]
  (let [p (:status resp)]
    (if (or
         (delivered? p)
         (not (failed? resp)))
      @p)))
