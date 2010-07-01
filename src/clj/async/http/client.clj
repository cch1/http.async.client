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

(defprotocol AsyncResponseHandler
  "Asynchronous response handler"
  (onStatus [state status] "On status received callback")
  (onHeaders [state headers] "On headers received callback")
  (onBodyPart [state part] "On body part (chunk) received callback")
  (onCompleted [state] "On response completed callback")
  (onError [state #^Throwable t] "On error callback"))

(deftype StoringHandler []
  AsyncResponseHandler
  (onStatus [state status] (status-collect state status))
  (onHeaders [state headers] (headers-collect state headers))
  (onBodyPart [state part] (body-collect state part))
  (onCompleted [state] (body-completed state))
  (onError [state t] (error-collect state t)))

(defn execute-request
  "Executes provided reqeust with given callback functions."
  ([#^Request req handler]
     (let [resp (promise)
           status (ref {:id (gensym "req-id__")})]
       (.executeRequest
        ahc req
        (proxy [AsyncHandler] []
          (onStatusReceived [#^HttpResponseStatus e]
                            (convert-action (onStatus handler status (convert-status-to-map e))))
          (onHeadersReceived [#^HttpResponseHeaders e]
                             (convert-action (onHeaders handler status (convert-headers-to-map e))))
          (onBodyPartReceived [#^HttpResponseBodyPart e]
                              (convert-action (onBodyPart handler status (vec (.getBodyPartBytes e)))))
          (onCompleted [] (deliver resp (onCompleted handler status)))
          (onThrowable [#^Throwable t]
                       (do
                         (onError handler status t)
                         (deliver resp (onCompleted status handler))))))))
  ([#^Request req]
     (execute-request req body-collect body-completed))
  ([#^Request req body-fn completed-fn]
     (execute-request req body-fn completed-fn headers-collect))
  ([#^Request req body-fn completed-fn headers-fn]
     (execute-request req body-fn completed-fn headers-fn status-collect))
  ([#^Request req body-fn completed-fn headers-fn status-fn]
     (execute-request req body-fn completed-fn headers-fn status-fn error-collect))
  ([#^Request req body-fn completed-fn headers-fn status-fn error-fn]
     (let [state (ref {:id (gensym "req-id-")})
           response (promise)]
       (.executeRequest
	ahc req
	(proxy [AsyncHandler] []
          (onStatusReceived [#^HttpResponseStatus resp]
                            (let [status (convert-status-to-map resp)]
                              (println "status")
                              (convert-action (status-fn state status))))
	  (onHeadersReceived [#^HttpResponseHeaders resp]
                             (let [hdrs (convert-headers-to-map resp)]
                               (println "headers")
                               (convert-action (headers-fn state hdrs))))
	  (onBodyPartReceived [#^HttpResponseBodyPart resp]
                              (let [part (vec (.getBodyPartBytes resp))]
                                (println "body")
                                (convert-action (body-fn state part))))
	  (onCompleted [] (do
                            (println "completed")
                            (deliver response (completed-fn state))))
	  (onThrowable [#^Throwable t]
                       (do
                         (println (str "error: " t))
                         (clojure.stacktrace/print-stack-trace t)
                         (error-fn state t)
                         (deliver response (completed-fn state))))))
       response)))
