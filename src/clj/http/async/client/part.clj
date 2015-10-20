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
  (:import (java.nio.charset Charset)
           (com.ning.http.client.multipart ByteArrayPart
                                           FilePart
                                           StringPart)))

(defn create-string-part
  "Create string multipart part"
  [{:keys [name value mime-type charset]}]
  (let [mime-type (or mime-type "text/plain")]
    (if charset
      (StringPart. name value mime-type (Charset/forName charset))
      (StringPart. name value mime-type))))

(defn create-file-part
  "Create file multipart part"
  [{:keys [name file mime-type charset]}]
  (FilePart. name file mime-type (Charset/forName charset)))

(defn create-bytearray-part
  "Create byte array multipart part"
  [{:keys [name file-name data mime-type charset]}]
  (ByteArrayPart. name data mime-type (Charset/forName charset) file-name))

(defn create-part
  "Create multipart part according to spec"
  [{type :type :as opts}]
  (case type
    :string    (create-string-part opts)
    :file      (create-file-part opts)
    :bytearray (create-bytearray-part opts)))
