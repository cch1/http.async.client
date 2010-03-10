(ns async.client
  
  (:import (com.ning.http.client AsyncHttpClient AsyncHandler
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart RequestBuilder
				 RequestType)))

(def ahc (AsyncHttpClient.))

(defstruct status :protocol :major :minor :code :text)

(defn statusCallback [status]
    (println (str "Status code: " @(status :code)))
    :abort)

(defn- defaultStatusCallback [status])

(defn convertMethod [method]
  "Converts clj method (:get, :put, ...) to Async Client specific.
   Defaults to :get"
  (cond
   (= method :get) RequestType/GET
   (= method :post) RequestType/POST
   (= method :put) RequestType/PUT
   (= method :delete) RequestType/DELETE
   (= method :head) RequestType/HEAD
   :default RequestType/GET))

(defn prepareRequest [method #^String url]
  (let [ahcMethod (convertMethod method)
	rb (RequestBuilder. ahcMethod)]
    (.. rb (setUrl url) (build))))

(defn prepareGet [#^String url]
  (prepareRequest :get url))

;; How do one make
;; (let [abc {:a (lazy)}])
;; ?
;; Answer: delay
(defn testRead [#^String url]
  (.executeRequest ahc (prepareGet url)
    (proxy [AsyncHandler] []
      (onStatusReceived [#^HttpResponseStatus response]
	(let [stat (struct status
			   (delay (.getProtocolName response))
			   (delay (.getProtocolMajorVersion response))
			   (delay (.getProtocolMinorVersion response))
			   (delay (.getStatusCode response))
			   (delay (.getStatusText response)))
	      action (statusCallback stat)]
	  (if (= :abort action)
	    com.ning.http.client.AsyncHandler$STATE/ABORT
	    com.ning.http.client.AsyncHandler$STATE/CONTINUE)))
      (onHeadersReceived [#^HttpResponseHeaders response]
	(do
	  (println response)
	  com.ning.http.client.AsyncHandler$STATE/ABORT))
      (onBodyPartReceived [#^HttpResponseBodyPart response]
	(do
	  (println response)
	  com.ning.http.client.AsyncHandler$STATE/ABORT))
      (onCompleted []
        (do
	  (println "Completed")
	  200))
      (onThrowable [#^Throwable t]
	(println (.getMessage t))))))
