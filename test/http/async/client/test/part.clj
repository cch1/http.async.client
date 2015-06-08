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

(ns http.async.client.test.part
  "Testing of http.async.client multipart."
  {:author "Hubert Iwaniuk"}
  (:require [clojure.test :refer :all]
            [http.async.client.part :refer :all])
  (:import (com.ning.http.client ByteArrayPart
                                 FilePart
                                 StringPart)
           (java.io File)))

(set! *warn-on-reflection* true)

(deftest string-part
  (testing "No encoding"
    (let [p (create-part {:type :string :name "test-name" :value "test-value"})]
      (is (instance? StringPart p))
      (are [expected tested] (= expected tested)
           "test-name" (.getName #^StringPart p)
           "test-value" (.getValue #^StringPart p)
           "UTF-8" (.getCharset #^StringPart p))))
  (testing "With encoding"
    (let [p (create-part {:type    :string
                          :name    "test-name"
                          :value   "test-value"
                          :charset "test-encoding"})]
      (is (instance? StringPart p))
      (are [expected tested] (= expected tested)
           "test-name" (.getName #^StringPart p)
           "test-value" (.getValue #^StringPart p)
           "test-encoding" (.getCharset #^StringPart p)))))

(deftest file-part
  (let [p (create-part {:type      :file
                        :name      "test-name"
                        :file      (File. "test-resources/test.txt")
                        :mime-type "text/plain"
                        :charset   "UTF-8"})]
    (is (instance? FilePart p))
    (are [expected tested] (= expected tested)
         "test-name" (.getName #^FilePart p)
         (File. "test-resources/test.txt") (.getFile #^FilePart p)
         "text/plain" (.getMimeType #^FilePart p)
         "UTF-8" (.getCharSet #^FilePart p))))

(deftest bytearray-part
  (let [p (create-part {:type      :bytearray
                        :name      "test-name"
                        :file-name "test-file-name"
                        :data       (.getBytes "test-content" "UTF-8")
                        :mime-type  "text/plain"
                        :charset    "UTF-8"})]
    (is (instance? ByteArrayPart p))
    (are [expected tested] (= expected tested)
         "test-name" (.getName #^ByteArrayPart p)
         "test-file-name" (.getFileName #^ByteArrayPart p)
         "test-content" (String. (.getData #^ByteArrayPart p))
         "text/plain" (.getMimeType #^ByteArrayPart p)
         "UTF-8" (.getCharSet #^ByteArrayPart p))))
