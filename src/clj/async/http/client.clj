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
  (:use [async.http.client status headers])
  (:import (com.ning.http.client AsyncHttpClient AsyncHandler Headers
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart Request RequestBuilder
				 RequestType)))

(def ahc (AsyncHttpClient.))

;; default set of callbacks
(defn body-collect [#^byteL bytes]
  ;; TODO append to rest of the body collected already
)

(defn body-completed []
  ;; TODO return completed body
)

(defn ignore-headers [id headers])

(defn print-headers [id headers]
  (println (str "bean: " (bean headers)))
  (doall (map #(println (str id "< " % ": " (get headers %))) (keys headers)))
  (if (not (contains? headers "content-type")) :abort ))

(defn accept-ok [id status]
  (if (not (= (:code status) 200)) :abort))

(defn print-status [id st]
  (println (str id "< "
		(:protocol st) "/" (:major st) "." (:minor st) " "
		(:code st) " " (:text st))))

(defn status-callback [id status]
    (println (str "Status code: " (status :code)))
    :abort)

(defn- default-status-callback [id status])

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
  "Executes provided reqeust with given callback functions."
  ([#^Request req]
     (execute-request req body-collect body-completed))
  ([#^Request req body-fn completed-fn]
     (execute-request req body-fn completed-fn print-headers))
  ([#^Request req body-fn completed-fn headers-fn]
     (execute-request req body-fn completed-fn headers-fn print-status))
  ([#^Request req body-fn completed-fn headers-fn status-fn]
     (let [id (gensym "req-id__")]
       (.executeRequest
	ahc req
	(proxy [AsyncHandler] []
	  (onStatusReceived [#^HttpResponseStatus resp]
                            (let [stat (convert-status-to-map resp)]
                              (convert-action (status-fn id stat))))
	  (onHeadersReceived [#^HttpResponseHeaders resp]
                             (let [hdrs (convert-headers-to-map resp)]
                               (convert-action (headers-fn id hdrs))))
	  (onBodyPartReceived [#^HttpResponseBodyPart response]
	    (do
	      (println :not-implemented)
	      com.ning.http.client.AsyncHandler$STATE/ABORT))
	  (onCompleted []
	    (do
	      (println "Completed")
	      200))
	  (onThrowable [#^Throwable t]
	    (println t)
	    (.printStackTrace t)))))))
