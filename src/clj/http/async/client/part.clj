; Copyright 2012 Hubert Iwaniuk
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

(ns http.async.client.part
  "Asynchronous HTTP Client - Clojure - Multipart API"
  {:author "Hubert Iwaniuk"}
  (:import (com.ning.http.client StringPart)))

(defn create-string-part
  "Create string multipart part"
  [{:keys [name value charset]}]
  (if charset
    (StringPart. name value charset)
    (StringPart. name value)))

(defn create-part
  "Create multipart part according to spec"
  [{type :type :as opts}]
  (case type
    :string (create-string-part opts)))
