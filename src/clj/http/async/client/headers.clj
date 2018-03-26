;;; Lazy headers.

;;; Copyright 2010 Hubert Iwaniuk

;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at

;;; http://www.apache.org/licenses/LICENSE-2.0

;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.
(ns http.async.client.headers "Asynchrounous HTTP Client - Clojure - Lazy headers"
    {:author "Hubert Iwaniuk"}
    (:import (com.ning.http.client HttpResponseHeaders FluentCaseInsensitiveStringsMap)))

(defn- kn [k]
  (if (keyword? k) (name k) k))

(defn- v [#^FluentCaseInsensitiveStringsMap h k]
  (let [vals (.get h (kn k))]
    (if (= 1 (count vals))
      (first vals)
      (vec vals))))

;; Convertion of AHC Headers to lazy map.
(defn convert-headers-to-map
  "Converts Http Response Headers to lazy map."
  [#^HttpResponseHeaders headers]
  (let [hds (.getHeaders headers)
        names (.keySet hds)]
    (proxy [clojure.lang.APersistentMap]
        []
      (containsKey [k] (.containsKey hds (kn k)))
      (entryAt [k] (when (.containsKey hds (kn k))
                     (proxy [clojure.lang.MapEntry]
                         [k nil]
                       (val [] (v hds k)))))
      (valAt
        ([k] (v hds k))
        ([k default] (if (.containsKey hds k)
                       (v hds k)
                       default)))
      (cons [m] (throw (UnsupportedOperationException. "Form 'cons' not supported: headers are read only.")))
      (count [] (.size hds))
      (assoc [k v] (throw (UnsupportedOperationException. "Form 'assoc' not supported: headers are read only.")))
      (without [k] (throw (UnsupportedOperationException. "Form 'without' not supported: headers are read only")))
      (seq [] ((fn thisfn [plseq]
                 (lazy-seq
                  (when-let [pseq (seq plseq)]
                    (let [k (keyword (.toLowerCase (first pseq)))]
                      (cons (proxy [clojure.lang.MapEntry]
                                [k nil]
                              (val [] (v hds k)))
                            (thisfn (rest pseq)))))))
               names)))))

;; Creates cookies from headers.
(defn create-cookies
  "Creates cookies from headers."
  [headers]
  (if (contains? headers :set-cookie)
    (for [cookie-string (let [set-cookie (:set-cookie headers)]
                          (if (string? set-cookie) (vector set-cookie) set-cookie))]
      (let [name-token (atom true)]
        (into {}
              (for [#^String cookie (.split cookie-string ";")]
                (let [keyval (map (fn [#^String x] (.trim x)) (.split cookie "=" 2))]
                  (if @name-token
                    (do
                      (compare-and-set! name-token true false)
                      {:name (first keyval) :value (second keyval)})
                    [(keyword (first keyval)) (second keyval)]))))))
    (println "No Set-Cookie header.")))
