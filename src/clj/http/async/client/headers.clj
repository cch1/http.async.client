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
  (:import (io.netty.handler.codec.http HttpHeaders)
           (clojure.lang MapEntry APersistentMap)))

(defn- ^String kn [k]
  (if (keyword? k) (name k) k))

;; Convertion of AHC Headers to lazy map.
(defn convert-headers-to-map
  "Converts Http Response Headers to lazy map."
  [#^HttpHeaders hds]
  (let [names (.names hds)]
    (proxy [APersistentMap]
        []
      (containsKey [k] (.contains hds (kn k)))
      (entryAt [k] (when (.contains hds (kn k))
                     (proxy [MapEntry]
                         [k nil]
                       (val [] (.get hds (kn k))))))
      (valAt
        ([k] (.get hds (kn k)))
        ([k default] (if (.contains hds (kn k))
                       (.get hds (kn k))
                       default)))
      (cons [m] (throw (UnsupportedOperationException. "Form 'cons' not supported: headers are read only.")))
      (count [] (.size names))
      (assoc [k v] (throw (UnsupportedOperationException. "Form 'assoc' not supported: headers are read only.")))
      (without [k] (throw (UnsupportedOperationException. "Form 'without' not supported: headers are read only")))
      (seq [] ((fn thisfn [plseq]
                 (lazy-seq
                  (when-let [pseq (seq plseq)]
                    (let [k (keyword (.toLowerCase (first pseq)))]
                      (cons (proxy [MapEntry]
                                [k nil]
                              (val [] (.get hds (kn k))))
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
