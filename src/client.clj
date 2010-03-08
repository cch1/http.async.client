(ns async.client
  
  (:import (com.ning.http.client AsyncHttpClient AsyncHandler
				 HttpResponseStatus HttpResponseHeaders
				 HttpResponseBodyPart)))

(def ahc (AsyncHttpClient.))

(defn statusReceived [status]
    (println (str "Status code: " (status :code)))
    :abort)

(defn prepareGet [#^String url]
  (.prepareGet ahc url))

;; How do one make
;; (let [abc {:a (lazy)}])
;; ?
(defn testRead [#^String url]
  (.execute (prepareGet url)
    (proxy [AsyncHandler] []
      (onStatusReceived [#^HttpResponseStatus response]
	(let [stat {:protocol
		    (do
		      (println "Getting protocol")
		      (.getProtocolName response))
		    :major (.getProtocolMajorVersion response)
		    :minor (.getProtocolMinorVersion response)
		    :code (.getStatusCode response)
		    :text (.getStatusText response)}
	      action (statusReceived stat)]
	  (if (= :abort stat)
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
