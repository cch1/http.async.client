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

(ns http.async.client.test.cert
  "Testing of http.async client x509 certificates"
  {:author "Hubert Iwaniuk / Andrew Diamond / RoomKey.com"}
  (:refer-clojure :exclude [await send])
  (:use clojure.test
        http.async.client
        http.async.client.cert
        [clojure.stacktrace :only [print-stack-trace]])
  (:import (com.ning.http.client AsyncHttpClient)
           (java.security KeyStore)
           (java.security.cert X509Certificate)
           (javax.net.ssl KeyManagerFactory SSLContext)
           (http.async.client.cert BlindTrustManager)))

(set! *warn-on-reflection* true)

(def ks-file "test-resources/keystore.jks")
(def cert-file "test-resources/certificate.crt")
(def authtype "RSA")
(def password "secret")
(def ks-cert-alias "my_test_cert")  ;; alias of cert built into key store
(def other-cert-alias "other_cert") ;; alias for certificate.crt

(defn load-test-certificate [] (load-x509-cert cert-file))

(defn load-test-keystore [] (load-keystore (resource-stream ks-file) password))

(deftest test-load-x509-cert
  (is (isa? (class (load-test-certificate)) X509Certificate)))

(deftest test-load-keystore
  (is (= KeyStore (class (load-test-keystore)))))

(deftest test-blind-trust-manager
  (let [b (BlindTrustManager.)
        chain (into-array X509Certificate (list (load-test-certificate)))]
    (is (nil? (.checkClientTrusted b chain authtype)))
    (is (nil? (.checkServerTrusted b chain authtype)))))

(deftest test-add-509-cert
  (let [ks (load-test-keystore)
        cert (load-test-certificate)
        ks (add-x509-cert ks other-cert-alias cert)]
    (is (= cert (.getCertificate ks other-cert-alias)))))

(deftest test-key-manager-factory
  (let [kmf (key-manager-factory (load-test-keystore) password)]
    (is (= KeyManagerFactory (class kmf)))))

(deftest test-ssl-context
  (let [ctx1 (ssl-context :keystore-file ks-file
                          :keystore-password password
                          :certificate-file cert-file
                          :certificate-alias other-cert-alias
                          :trust-managers [(BlindTrustManager.)])
        ;; Make sure it works without :trust-managers param
        ctx2 (ssl-context :keystore-file ks-file
                          :keystore-password password
                          :certificate-file cert-file
                          :certificate-alias other-cert-alias)]
    (is (= SSLContext (class ctx1)))
    (is (= SSLContext (class ctx2)))))

(deftest test-client
  (let [ctx (ssl-context :keystore-file ks-file
                         :keystore-password password
                         :certificate-file cert-file
                         :certificate-alias other-cert-alias)]
    (with-open [client (create-client :ssl-context ctx)]
      (is (= AsyncHttpClient (class client)))
      ;; Make sure client is using the SSLContext we supplied
      (is (= ctx (.getSSLContext (.getConfig client)))))))
