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

(ns async.http.client.util
  "Async HTTP Client - Clojure - Utils"
  {:author "Hubert Iwaniuk"})

(defn promise
  "Alpha - subject to change.
  Returns a promise object that can be read with deref/@, and set,
  once only, with deliver. Calls to deref/@ prior to delivery will
  block. All subsequent derefs will return the same delivered value
  without blocking."
  {:added "1.1"}
  []
  (let [d (java.util.concurrent.CountDownLatch. 1)
        v (atom nil)]
    ^{:delivered?
      (fn []
        (locking d
          (zero? (.getCount d))))}
    (reify 
     clojure.lang.IDeref
      (deref [_] (.await d) @v)
     clojure.lang.IFn
      (invoke [this x]
        (locking d
          (if (pos? (.getCount d))
            (do (reset! v x)
                (.countDown d)
                this)
            (throw (IllegalStateException. "Multiple deliver calls to a promise"))))))))

(defn delivered?
  "Alpha - subject to change.
  Returns true if promise has been delivered, else false"
  [p]
  ((:delivered? (meta p))))
