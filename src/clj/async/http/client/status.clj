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
