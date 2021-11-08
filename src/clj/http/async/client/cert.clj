;;; Copyright 2012 Hubert Iwaniuk

;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at

;;; http://www.apache.org/licenses/LICENSE-2.0

;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.
(ns http.async.client.cert
  "Asynchronous HTTP Client - Clojure - Utils"
  {:author "Hubert Iwaniuk / Andrew Diamond / RoomKey.com"}
  (:import (java.security
            KeyStore
            SecureRandom)
           (java.security.cert
            X509Certificate
            CertificateFactory)
           (javax.net.ssl
            KeyManagerFactory
            SSLContext
            HttpsURLConnection
            X509TrustManager)
           (io.netty.handler.ssl
             SslContext)
           (java.io
            File
            InputStream
            FileInputStream
            InputStreamReader
            FileNotFoundException))
  (:require [clojure.java.io :as io]))

(defrecord BlindTrustManager
    ;;   "An X509TrustManager that blindly trusts all certificates, chains and issuers.
    ;;    All methods return nil, indicating that the client/server is trusted."
    []
  X509TrustManager
  (checkClientTrusted [this chain auth-type] nil)
  (checkServerTrusted [this chain auth-type] nil)
  (getAcceptedIssuers [this] nil))

(defn #^InputStream load-embedded-resource
  "Loads a resource embedded in a jar file. Returns an InputStream"
  [#^String resource]
  (io/input-stream (io/resource resource)))

(defn #^InputStream resource-stream
  "Loads the resource at the specified path, and returns it as an
   InputStream. If there is no file at the specified path, and we
   are running as a jar, we'll attempt to load the resource embedded
   within the jar at the specified path."
  [#^String path]
  (try
    (if (.exists (io/file path))
      (FileInputStream. path)
      (load-embedded-resource path))
    (catch Exception _ (throw (new FileNotFoundException
                                   (str "File or resource \""
                                        path
                                        "\" could not be found."))))))

(defn #^X509Certificate load-x509-cert
  "Loads an x509 certificate from the specified path, which may be either
   a file system path or a path to an embedded resource in a jar file.
   Returns an instace of java.security.cert.X509Certificate."
  [#^String path]
  (let [cert-file-instream (resource-stream path)
        cert-factory (CertificateFactory/getInstance "X.509")
        cert (.generateCertificate cert-factory cert-file-instream)]
    (.close cert-file-instream)
    (cast X509Certificate cert)))

(defn #^KeyStore load-keystore
  "Loads a KeyStore from the specified file. Param keystore-stream is
   an InputStream.  If password is provided, that will be used to unlock
   the KeyStore. Password may be nil. If keystore-stream is nil, this
   returns an empty default KeyStore."
  [#^FileInputStream keystore-stream #^String password]
  (let [ks (KeyStore/getInstance (KeyStore/getDefaultType))]
    (if keystore-stream
      (if password
        (.load ks keystore-stream (.toCharArray password))
        (.load ks keystore-stream nil))
      (.load ks nil nil))
    ks))

(defn #^KeyStore add-x509-cert
  "Adds the x509 certificate to the specified keystore. Param cert-alias
   is a name for this cert. Returns KeyStore with the certificate loaded."
  [#^KeyStore keystore #^String cert-alias #^String certificate]
  (.setCertificateEntry keystore cert-alias certificate)
  keystore)

(defn #^KeyManagerFactory key-manager-factory
  "Returns a key manager for X509 certs using the speficied keystore."
  [#^KeyStore keystore #^String password]
  (let [kmf (KeyManagerFactory/getInstance "SunX509")]
    (if password
      (.init kmf keystore (.toCharArray password))
      (.init kmf keystore nil))
    kmf))

(defn #^SSLContext ssl-context
  "Creates a new SSLContext with x509 certificates. This allows you to use client
   certificates in your async http requests.

   File params should be relative to resources when your app is running as a jar.
   For example, if your certificate file is in resources/security/mycert.pem,
   then the :certificate-file param would be \"security/mycert.pem\".

   :keystore-file - Path to Java keystore containing any private keys and
   trusted certificate authority certificates required for this connection.
   If this is nil or missing, a default keystore will be used.

   :keystore-password - Password to unlock KeyStore if keystore file is provided.

   :certificate-file The path to the file containing an X509 certificate
   (or certificate chain) to be used in the https connection

   :certificate-alias - A name by which to access an X509 certificate that will
   be loaded into the KeyStore.

   :trust-managers - [optional] A seq of javax.net.ssl.X509TrustManager objects.
   These are used to verify the certificates sent by the remote host. If
   you don't specify this option, the connection will use an instance of
   BlindTrustManager, which blindly trusts all certificates. This is handy,
   but it's not particularly safe."
  [& {:keys [keystore-file
             keystore-password
             certificate-alias
             certificate-file
             trust-managers]}]
  (let [initial-keystore (load-keystore
                          (when keystore-file (resource-stream keystore-file))
                          keystore-password)
        keystore-with-cert (add-x509-cert
                            initial-keystore
                            certificate-alias
                            (load-x509-cert certificate-file))
        key-mgr-factory (key-manager-factory keystore-with-cert keystore-password)
        ctx (SSLContext/getInstance "TLS")
        key-managers (.getKeyManagers key-mgr-factory)
        trust-managers (into-array javax.net.ssl.X509TrustManager
                                   (or trust-managers
                                       (list (new BlindTrustManager))))]
    (.init ctx key-managers trust-managers (new SecureRandom))
    ctx))
