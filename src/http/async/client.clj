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

(ns http.async.client
  "Asynchronous HTTP Client - Clojure"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [await promise])
  (:require [clojure.contrib.io :as duck])
  (:use [http.async.client request headers util]
        clojure.template)
  (:import (java.io ByteArrayOutputStream)
           (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder)))

(defn create-client
  "Creates new Async Http Client.
  Arguments:
  - :compression-enabled :: enable HTTP compression
  - :connection-timeout :: connections timeout in ms
  - :follow-redirects :: enable following HTTP redirects
  - :idle-timeout :: idle connection timeout in ms
  - :keep-alive :: enable HTTP keep alive, enabled by default
  - :max-conns-per-host :: max number of polled connections per host
  - :max-conns-total :: max number of total connections held open by client
  - :max-redirects :: max nuber of redirects to follow
  - :request-timeout :: request timeout in ms
  - :user-agent :: User-Agent branding string"
  [& {:keys [compression-enabled
             connection-timeout
             follow-redirects             
             idle-timeout
             keep-alive
             max-conns-per-host
             max-conns-total
             max-redirects
             ;; proxy-server
             ;; realm
             request-timeout
             user-agent]}]
  (AsyncHttpClient.
   (.build
    (let [b (AsyncHttpClientConfig$Builder.)]
      (when compression-enabled (.setCompressionEnabled b compression-enabled))
      (when connection-timeout (.setConnectionTimeoutInMs b connection-timeout))
      (when follow-redirects (.setFollowRedirects b follow-redirects))
      (when idle-timeout (.setIdleConnectionTimeoutInMs b idle-timeout))
      (when keep-alive (.setKeepAlive b keep-alive))
      (when max-conns-per-host (.setMaximumConnectionsPerHost b max-conns-per-host))
      (when max-conns-total (.setMaximumConnectionsTotal b max-conns-total))
      (when max-redirects (.setMaximumNumberOfRedirects b max-redirects))
      (when request-timeout (.setRequestTimeoutInMs b request-timeout))
      (.setUserAgent b (if user-agent user-agent *user-agent*))
      b))))

(defmacro with-client
  "Creates new Async Http Client with given configuration
  than executes body and closes the client.
  Config accepts same configuration options as create-client."
  [config & body]
  `(with-open [c# (create-client ~@(apply concat config))]
     (binding [*client* c#]
       ~@body)))

(gen-methods :get :post :put :delete :head :options)

(defn request-stream
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  [method #^String url body-part-callback & {:as options}]
  (apply execute-request
         (apply prepare-request method url (apply concat options))
         (apply concat (merge *default-callbacks* {:part body-part-callback}))))

(defn stream-seq
  "Creates potentially infinite lazy sequence of Http Stream."
  [method #^String url & {:as options}]
  (let [que (java.util.concurrent.LinkedBlockingQueue.)
        s-seq ((fn gen-next []
                 (lazy-seq
                  (let [v (.take que)]
                    (when-not (= ::done v)
                      (cons v (gen-next)))))))]
    (apply execute-request
           (apply prepare-request method url (apply concat options))
           (apply concat (merge
                          *default-callbacks*
                          {:part (fn [_ baos]
                                   (.put que baos)
                                   [s-seq :continue])
                           :completed (fn [_] (.put que ::done))
                           :error (fn [_ t]
                                    (.put que ::done)
                                    t)})))))

(defn failed?
  "Checks if request failed."
  [resp]
  (delivered? (:error resp)))

(defn done?
  "Checks if request is finished already (response receiving finished)."
  [resp]
  (delivered? (:done resp)))

(defn- safe-get
  [k r]
  (let [p (k r)]
    (if (or
         (delivered? p)
         (not
          (or
           (failed? r)
           (done? r))))
      @p)))

(defn await
  "Waits for response processing to be finished.
  Returns same response."
  [response]
  (safe-get :done response)
  response)

(defn headers
  "Gets headers.
  If headers have not yet been delivered and request hasn't failed waits for headers."
  [resp]
  (safe-get :headers resp))

(defn body
  "Gets body.
  If body have not yet been delivered and request hasn't failed waits for body."
  [resp]
  (safe-get :body resp))

(defn string
  "Converts response to string."
  [resp]
  (let [enc (or (get-encoding (headers resp)) duck/*default-encoding*)
        convert (fn [#^ByteArrayOutputStream baos] (.toString baos enc))]
    (when-let [body (body resp)]
      (if (seq? body)
        (map convert body)
        (convert body)))))

(defn cookies
  "Gets cookies from response."
  [resp]
  (create-cookies (headers resp)))

(defn status
  "Gets status if status was delivered."
  [resp]
  (safe-get :status resp))

(defn error
  "Returns Throwable if request processing failed."
  [resp]
  (if (failed? resp)
    @(:error resp)))

(defn cancelled?
  "Checks if response has been cancelled."
  [resp]
  (when-let [f (:cancelled? (meta resp))]
    (f)))

(defn cancel
  "Cancels response."
  [resp]
  (when-let [f (:cancel (meta resp))]
    (f)))

(defn close
  "Closes client."
  ([] (close *client*))
  ([client] (.close client)))
