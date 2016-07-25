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
  (:import (com.ning.http.client ProxyServer
                                 ProxyServer$Protocol
                                 Realm$AuthScheme
                                 Realm$RealmBuilder)))

(defn- proto-map [proto]
  (if proto
    ({:http  ProxyServer$Protocol/HTTP
      :https ProxyServer$Protocol/HTTPS} proto)
    ProxyServer$Protocol/HTTP))

(defn set-proxy
  "Sets proxy on builder.
  Note that in v1.0.0 you must also set a realm to enable HTTPS
  traffic via the proxy."
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
  "Sets realm on builder.
  Note that in v.1.0.0 you must set a realm to enable HTTPS traffic
  via the proxy."
  [{:keys [type user password realm preemptive target-proxy]
    :or {type :basic}} b]
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
    (when-not (nil? preemptive)
      (.setUsePreemptiveAuth rbld preemptive))
    (when-not (nil? target-proxy)
      (.setTargetProxy rbld target-proxy))
    (doto rbld
      (.setPrincipal user)
      (.setPassword password))
    (.setRealm b (.build rbld))))
