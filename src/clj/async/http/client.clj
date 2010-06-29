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
(defn body-collect [id #^byteL bytes]
  ;; TODO append to rest of the body collected already
)

(defn body-completed []
  ;; TODO return completed body
)

(defn ignore-headers [id headers])

(defn print-headers [id headers]
  (doall (map #(println (str id "< " % ": " (get headers %))) (keys headers))))

(defn accept-ok [id status]
  (if (not (= (:code status) 200)) :abort))

(defn print-status [id st]
  (println (str id "< " (:protocol st) " " (:code st) " " (:msg st))))

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
     (let [id (gensym "req-id__")
           status (ref nil)
           headers (ref nil)
           body (ref (vector))
           response (promise)]
       (.executeRequest
	ahc req
	(proxy [AsyncHandler] []
	  (onStatusReceived [#^HttpResponseStatus resp]
                            (let [stat (convert-status-to-map resp)]
                              (dosync (ref-set status stat))
                              (convert-action (status-fn id stat))))
	  (onHeadersReceived [#^HttpResponseHeaders resp]
                             (let [hdrs (convert-headers-to-map resp)]
                               (dosync (ref-set headers hdrs))
                               (convert-action (headers-fn id hdrs))))
	  (onBodyPartReceived [#^HttpResponseBodyPart resp]
                              (let [#^byteL part (.getBodyPartBytes resp)
                                    v (seq part)]
                                (dosync (alter body #(apply conj %1 %2) v))
                                (convert-action (body-fn id part))))
	  (onCompleted [] (deliver response {:status @status :headers @headers :body @body}))
	  (onThrowable [#^Throwable t]
                       (println t)
                       (.printStackTrace t))))
       response)))
