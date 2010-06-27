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

(ns async.http.client.headers
  (:import (com.ning.http.client HttpResponseHeaders Headers)))

(defn- kn [k]
  (if (keyword? k) (name k) k))

(defn- v [#^Headers h k]
  (let [vals (.getHeaderValues h (kn k))]
    (reduce (fn [n m] (str n "," m)) vals)))

(defn convert-headers-to-map [#^HttpResponseHeaders headers]
  "Converts Http Response Headers to lazy map."
  (let [hds (.getHeaders headers)
        names (.getHeaderNames hds)]
    (proxy [clojure.lang.APersistentMap]
        []
      (containsKey [k] (contains? names (kn k)))
      (entryAt [k] (when (contains? names (kn k))
                     (proxy [clojure.lang.MapEntry]
                         [k nil]
                       (val [] (v hds k)))))
      (valAt
       ([k] (v hds k))
       ([k default] (if (contains? names k)
                      (v hds k)
                      default)))
      (cons [m] (throw "Headers are read only."))
      (count [] (count names))
      (assoc [k v] (throw "Headers are read only."))
      (without [k] (throw "Headers are read only"))
      (seq [] ((fn thisfn [plseq]
                  (lazy-seq
                   (when-let [pseq (seq plseq)]
                     (let [k (keyword (.toLowerCase (first pseq)))]
                       (cons (proxy [clojure.lang.MapEntry]
                                 [k nil]
                               (val [] (v hds k)))
                             (thisfn (rest pseq)))))))
               names)))))
