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

(ns http.async.client.timing
  "Asynchronous HTTP Client - Clojure - Response Timing API"
  {:author "Hubert Iwaniuk"})

(defn status-time
  "Time it took (in milliseconds) from request execution to receiving status line."
  [response]
  (/ (double (- @(:status-time response) @(:started-time response))) 1000000.0))

(defn headers-time
  "Time it took (in milliseconds) from request execution to receiving headers."
  [response]
  (/ (double (- @(:headers-time response) @(:started-time response))) 1000000.0))

(defn body-time
  "Time it took (in milliseconds) from request execution to receiving body."
  [response]
  (/ (double (- @(:body-time response) @(:started-time response))) 1000000.0))

(defn done-time
  "Time it took (in milliseconds) from request execution to completing response."
  [response]
  (/ (double (- @(:done-time response) @(:started-time response))) 1000000.0))

(defn error-time
  "Time it took (in milliseconds) from request execution to failure."
  [response]
  (/ (double (- @(:error-time response) @(:started-time response))) 1000000.0))
