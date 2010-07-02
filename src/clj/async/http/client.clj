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
  (:use (async.http.client status headers)
        clojure.stacktrace)
  (:import (com.ning.http.client AsyncHttpClient AsyncHandler Headers
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart Request RequestBuilder
				 RequestType)))

(def ahc (AsyncHttpClient.))

;; default set of callbacks
(defn body-collect [state bytes]
  "Stores body parts under :body in state."
  (if bytes
    (dosync (alter state assoc :body (apply conj (or (@state :body) []) bytes)))
    ((println "Received empty body part, aborting.") :abort)))

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
  "Storest status map under :status in state."
  (dosync (alter state assoc :status status)))

(defn status-print [state st]
  (println (str (:id @state) "< " (:protocol st) " " (:code st) " " (:msg st))))

(defn error-collect [state t]
  "Stores exception under :error in state"
  (dosync (alter state assoc :error t)))

(defn- convert-method [method]
  "Converts clj method (:get, :put, ...) to Async Client specific.
   Defaults to :get"
  (cond
   (= method :get) RequestType/GET
   (= method :post) RequestType/POST
   (= method :put) RequestType/PUT
   (= method :delete) RequestType/DELETE
   (= method :head) RequestType/HEAD
   :default RequestType/GET))

(defn prepare-request [method #^String url]
  "Prepares request."
  {:tag Request}
  (let [mtd (convert-method method)
	rb (RequestBuilder. mtd)]
    (.. rb (setUrl url) (build))))

(defn prepare-get [#^String url]
  "Prepares GET reqeust for given url."
  {:tag Request}
  (prepare-request :get url))

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
                               (convert-action (pt-cb state (vec (.getBodyPartBytes e)))))
          (onCompleted []
                       (deliver resp (ct-cb state)))
          (onThrowable [#^Throwable t]
                       (do (er-cb state t) (deliver resp (ct-cb state))))))
       resp)))

(defn GET [#^String url]
  "GET resource from url. Return promise, that is delivered once, response is completed."
  (execute-request (prepare-get url) {:status status-collect
                                      :headers headers-collect
                                      :part body-collect
                                      :completed body-completed
                                      :error error-collect}))
