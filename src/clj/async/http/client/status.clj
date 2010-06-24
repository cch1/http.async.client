(ns async.http.client.status
  (:import (com.ning.http.client HttpResponseStatus)))

(defn convert-status-to-map [st]
  (let [lm {:code (delay (.getStatusCode st))
	    :text (delay (.getStatusText st))
	    :protocol (delay (.getProtocolText st))
	    :major (delay (.getProtocolMajorVersion st))
	    :minor (delay (.getProtocolMinorVersion st))}]
    (proxy [clojure.lang.APersistentMap]
	[]
      (containsKey [k] (contains? lm k))
      (entryAt [k] (when (contains? lm k)
		     (proxy [clojure.lang.MapEntry]
			 [k nil]
		       (val [] (let [v (lm k)]
				 (if (delay? v) @v v))))))
      (valAt
	([k] (let [v (lm k)]
	       (if (delay? v) @v v)))
	([k default] (if (contains? lm k)
		       (let [v (lm k)]
			 (if (delay? v) @v v))
		       default)))
      (cons [m] (conj lm m))
      (count [] (count lm))
      (assoc [k v] (assoc lm k v))
      (without [k] (dissoc lm k))
      (seq [] ((fn thisfn [plseq]
		 (lazy-seq
		  (when-let [pseq (seq plseq)]
		    (cons (proxy [clojure.lang.MapEntry]
			      [(first pseq) nil]
			    (val [] (let [v (lm (first pseq))]
				      (if (delay? v) @v v))))
			  (thisfn (rest pseq))))))
	       (keys lm))))))
