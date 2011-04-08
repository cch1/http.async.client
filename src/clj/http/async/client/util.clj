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

(ns http.async.client.util
  "Asynchronous HTTP Client - Clojure - Utils"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [promise])
  (:import (com.ning.http.client ProxyServer
                                 ProxyServer$Protocol
                                 Realm$AuthScheme
                                 Realm$RealmBuilder)))

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
  (if-let [f (:delivered? (meta p))]
   (f)))

(defn- proto-map [proto]
  (if proto
    ({:http  ProxyServer$Protocol/HTTP
      :https ProxyServer$Protocol/HTTPS} proto)
    ProxyServer$Protocol/HTTP))

(defn set-proxy
  "Sets proxy on builder."
  [{:keys [protocol host port user password]} b]
  {:pre [(or (nil? protocol)
             (contains? #{:http :https} protocol))
         host port
         (or (and (nil? user) (nil? password))
             (and user password))]}
  (.setProxyServer b (if user
                       (ProxyServer. (proto-map protocol) host port user password)
                       (ProxyServer. (proto-map protocol) host port))))

(defn- type->auth-scheme [type]
  (or ({:basic Realm$AuthScheme/BASIC
        :digest Realm$AuthScheme/DIGEST} type)
      Realm$AuthScheme/BASIC))

(defn set-realm
  "Sets realm on builder."
  [{:keys [type user password realm preemptive]
    :or {:type :basic}} b]
  (let [rbld (Realm$RealmBuilder.)]
    (.setScheme rbld (type->auth-scheme type))
    (when (nil? user)
      (if (nil? password)
        (throw (IllegalArgumentException. "For authentication user and password is required"))
        (throw (IllegalArgumentException. "For authentication user is required"))))
    (when (nil? password)
      (throw (IllegalArgumentException. "For authentication password is required")))
    (when (= :digest type)
      (when (nil? realm) (throw (IllegalArgumentException.
                                 "For DIGEST authentication realm is required")))
      (.setRealmName rbld realm))
    (when (not (nil? preemptive))
      (.setUsePreemptiveAuth rbld preemptive))
    (doto rbld
      (.setPrincipal user)
      (.setPassword password))
    (.setRealm b (.build rbld))))
