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
  (:use clojure.test
        http.async.client.part)
  (:import (com.ning.http.client StringPart)))

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
    (let [p (create-part {:type :string
                          :name "test-name"
                          :value "test-value"
                          :charset "test-encoding"})]
      (is (instance? StringPart p))
      (are [expected tested] (= expected tested)
           "test-name" (.getName #^StringPart p)
           "test-value" (.getValue #^StringPart p)
           "test-encoding" (.getCharset #^StringPart p)))))
