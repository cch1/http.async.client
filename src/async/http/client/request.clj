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
  (:require [clojure.contrib.duck-streams :as duck])
  (:use [async.http.client status headers]
        [clojure.stacktrace]
        [clojure.contrib.java-utils :only [as-str]]
        [clojure.contrib.str-utils :only [str-join]])
  (:import (com.ning.http.client AsyncHttpClient AsyncHandler Headers
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart Request RequestBuilder
				 RequestType)
           (ahc RequestBuilderWrapper)
           (java.net URLEncoder)
           (java.io InputStream)))

(def ahc (AsyncHttpClient.))

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

;; default set of callbacks
(defn body-collect [state bytes]
  "Stores body parts under :body in state."
  (if (not (empty? bytes))
    (dosync (alter state assoc :body (apply conj (or (:body @state) []) bytes)))
    (do (println "Received empty body part."))))

(defn body-completed [state]
  "Provides value that will be delivered to response promise."
  @state)

(defn headers-collect [state headers]
  "Stores headers under :headers in state."
  (if headers
    (dosync (alter state assoc :headers headers))
    ((println "Received empty headers, aborting.") :abort)))

(defn print-headers [state headers]
  (doall (map #(println (str (:id @state) "< " % ": " (get headers %))) (keys headers))))

(defn accept-ok [_ status]
  (if (not (= (:code status) 200)) :abort))

(defn status-collect [state status]
  "Stores status map under :status in state."
  (dosync (alter state assoc :status status)))

(defn status-print [state st]
  (println (str (:id @state) "< " (:protocol st) " " (:code st) " " (:msg st))))

(defn error-collect [state t]
  "Stores exception under :error in state"
  (dosync (alter state assoc :error t)))

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
    :param   - map of parameters
    :headers - map of headers"
  {:tag Request}
  ([method #^String url]
     (prepare-request method url {}))
  ([method #^String url options]
     (prepare-request method url options nil))
  ([method
    #^String url
    {headers :headers
     param :param
     query :query
     :as options}
    body]
     ; RequestBuilderWrapper is needed for now, until RequestBuilder
     ; is able to be used directly from Clojure.
     (let [#^RequestBuilderWrapper rbw
           (RequestBuilderWrapper.
            (RequestBuilder. (convert-method method)))]
       (doseq [[k v] headers] (.addHeader rbw
                                          (if (keyword? k) (name k) k)
                                          (str v)))
       (doseq [[k v] param] (.addParameter rbw
                                           (if (keyword? k) (name k) k)
                                           (str v)))
       (doseq [[k v] query] (.addQueryParameter rbw
                                                (if (keyword? k) (name k) k)
                                                (str v)))
       (if body
         (if (map? body)
           (doseq [[k v] body]
             (.addParameter rbw
                            (if (keyword? k) (name k) k)
                            (str v)))
           (.setBody
            rbw
            (cond
             (string? body) (.getBytes (url-encode body) "UTF-8")
             (instance? InputStream body) body))))
       (.. (.getRequestBuilder rbw) (setUrl url) (build)))))

(defn prepare-get
  "Prepares GET reqeust for given url."
  {:tag Request}
  ([#^String url]
     (prepare-get url {}))
  ([#^String url options]
     (prepare-request :get url options)))

(defn prepare-post
  "Prepares POST request to given url."
  {:tag Request}
  ([#^String url]
     (prepare-post url {}))
  ([#^String url options]
     (prepare-post url options nil))
  ([#^String url options body]
     (prepare-request :post url options body)))

(defn prepare-put
  "Prepares PUT request to given url."
  {:tag Request}
  ([#^String url]
     (prepare-put url {}))
  ([#^String url options]
     (prepare-put url options nil))
  ([#^String url options body]
     (prepare-request :put url options body)))

(defn prepare-delete
  "Prepares DELETE request to given url."
  {:tag Request}
  ([#^String url]
     (prepare-delete url {}))
  ([#^String url options]
     (prepare-delete url options nil))
  ([#^String url options body]
     (prepare-request :delete url options body)))

(defn convert-action
  "Converts action (:abort, nil) to Async client STATE."
  [action]
  {:tag com.ning.http.client.AsyncHandler$STATE}
  (if (= action :abort)
    com.ning.http.client.AsyncHandler$STATE/ABORT
    com.ning.http.client.AsyncHandler$STATE/CONTINUE))

(defn execute-request
  "Executes provided reqeust with given callback functions.
  Options:
    :status    - Status callback, fn called with state (ref {}) and status map.
    :headers   - Headers callback, fn called with state (ref {}) and headers map.
    :part      - Body part callback, fn called with state (ref {}) and vector of bytes.
    :completed - Request completed callback, fn called with state (ref {}), result is delivered to response promise..
    :error     - Error callback, fn called with state (ref {}) and Throwable."
  ([#^Request req options]
     (let [resp (promise)
           state (ref {:id (gensym "req-id__")})
           st-cb (:status options)
           hd-cb (:headers options)
           pt-cb (:part options)
           ct-cb (:completed options)
           er-cb (:error options)]
       (.executeRequest
        ahc req
        (proxy [AsyncHandler] []
          (onStatusReceived [#^HttpResponseStatus e]
                            (convert-action (st-cb state (convert-status-to-map e))))
          (onHeadersReceived [#^HttpResponseHeaders e]
                             (convert-action (hd-cb state (convert-headers-to-map e))))
          (onBodyPartReceived  [#^HttpResponseBodyPart e]
                               (when-let [vb (vec (.getBodyPartBytes e))]
                                 (convert-action (pt-cb state vb))))
          (onCompleted []
                       (deliver resp (ct-cb state)))
          (onThrowable [#^Throwable t]
                       (do (er-cb state t) (deliver resp @state)))))
       resp)))
