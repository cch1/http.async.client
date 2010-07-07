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
  (:use (async.http.client request)))

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

(defn GET
  "GET resource from url. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (GET url {}))
  ([#^String url options]
     (execute-request (prepare-get url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))

(defn POST
  "POST to resource. Returns promise, that is delivered once response is completed."
  ([#^String url]
     (POST url []))
  ([#^String url body]
     (POST url body {}))
  ([#^String url body options]
     (execute-request (prepare-post url options)
                      {:status status-collect
                       :headers headers-collect
                       :part body-collect
                       :completed body-completed
                       :error error-collect})))
