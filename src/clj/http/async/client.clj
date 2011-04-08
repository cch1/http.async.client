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
  (:use [http.async.client request headers util])
  (:import (java.io ByteArrayOutputStream)
           (java.util.concurrent LinkedBlockingQueue)
           (com.ning.http.client AsyncHttpClient AsyncHttpClientConfig$Builder)
           (com.ning.http.client.providers.netty NettyAsyncHttpProviderConfig)))

(defn create-client
  "Creates new Async Http Client.
  Arguments:
  - :compression-enabled :: enable HTTP compression
  - :connection-timeout :: connections timeout in ms
  - :follow-redirects :: enable following HTTP redirects
  - :idle-in-pool-timeout :: idle connection in pool timeout in ms
  - :keep-alive :: enable HTTP keep alive, enabled by default
  - :max-conns-per-host :: max number of polled connections per host
  - :max-conns-total :: max number of total connections held open by client
  - :max-redirects :: max nuber of redirects to follow
  - :proxy :: map with proxy configuration to be used
      :host     - proxy host
      :port     - proxy port
      :protocol - (optional) protocol to communicate with proxy,
                  :http (default, if you provide no value) and :https are allowed
      :user     - (optional) user name to use for proxy authentication,
                  has to be provided with :password
      :password - (optional) password to use for proxy authentication,
                  has to be provided with :user
  - :auth :: map with authentication to be used
      :type       - either :basic or :digest
      :user       - user name to be used
      :password   - password to be used
      :realm      - realm name to authenticate in
      :preemptive - assume authentication is required
  - :request-timeout :: request timeout in ms
  - :user-agent :: User-Agent branding string
  - :async-connect :: Execute connect asynchronously
  - :executor-service :: provide your own executor service for callbacks to be executed on"
  {:tag AsyncHttpClient}
  [& {:keys [compression-enabled
             connection-timeout
             follow-redirects             
             idle-in-pool-timeout
             keep-alive
             max-conns-per-host
             max-conns-total
             max-redirects
             proxy
             auth
             request-timeout
             user-agent
             async-connect
             executor-service]}]
  (AsyncHttpClient.
   (.build
    (let [b (AsyncHttpClientConfig$Builder.)]
      (when-not (nil? compression-enabled) (.setCompressionEnabled b compression-enabled))
      (when connection-timeout (.setConnectionTimeoutInMs b connection-timeout))
      (when-not (nil? follow-redirects) (.setFollowRedirects b follow-redirects))
      (when idle-in-pool-timeout (.setConnectionTimeoutInMs b idle-in-pool-timeout))
      (when-not (nil? keep-alive) (.setAllowPoolingConnection b keep-alive))
      (when max-conns-per-host (.setMaximumConnectionsPerHost b max-conns-per-host))
      (when max-conns-total (.setMaximumConnectionsTotal b max-conns-total))
      (when max-redirects (.setMaximumNumberOfRedirects b max-redirects))
      (when async-connect
        (let [provider-config (doto (NettyAsyncHttpProviderConfig.)
                                (.removeProperty NettyAsyncHttpProviderConfig/USE_BLOCKING_IO)
                                (.addProperty NettyAsyncHttpProviderConfig/EXECUTE_ASYNC_CONNECT true))]
          (.setAsyncHttpClientProviderConfig b provider-config)))
      (when executor-service (.setExecutorService b executor-service))
      (when proxy
        (set-proxy proxy b))
      (when auth
        (set-realm auth b))
      (when request-timeout (.setRequestTimeoutInMs b request-timeout))
      (.setUserAgent b (if user-agent user-agent *user-agent*))
      b))))

(defmacro with-client
  "Creates new Async Http Client with given configuration
  than executes body and closes the client.
  Config accepts same configuration options as create-client."
  [config & body]
  `(with-open [c# (create-client ~@(apply concat config))]
     (binding [http.async.client.request/*CLIENT* c#]
       ~@body)))

(defmacro gen-methods [& methods]
  (list* 'do
     (map (fn [method#]
            (let [fn-name (symbol (.toUpperCase (name method#)))
                  fn-doc (str "Sends asynchronously HTTP " fn-name " request to url.
  Returns a map:
  - :id      - unique ID of request
  - :status  - promise that once status is received is delivered, contains lazy map of:
    - :code     - response code
    - :msg      - response message
    - :protocol - protocol with version
    - :major    - major version of protocol
    - :minor    - minor version of protocol
  - :headers - promise that once headers are received is delivered, contains lazy map of:
    - :server - header names are keyworded, values stay not changed
  - :body    - body of response, depends on request type, might be ByteArrayOutputStream
               or lazy sequence, use conveniece methods to extract it, like string
  - :done    - promise that is delivered once receiving response has finished
  - :error   - promise that is delivered if requesting resource failed, once delivered
               will contain Throwable.
  Arguments:
  - client   - client created via create-client
  - url      - URL to request
  - options  - keyworded arguments:
    :query   - map of query parameters
    :headers - map of headers
    :body    - body
    :cookies - cookies to send
    :proxy   - map with proxy configuration to be used
      :host     - proxy host
      :port     - proxy port
      :protocol - (optional) protocol to communicate with proxy,
                  :http (default, if you provide no value) and :https are allowed
      :user     - (optional) user name to use for proxy authentication,
                  has to be provided with :password
      :password - (optional) password to use for proxy authentication,
                  has to be provided with :user
    :auth    - map with authentication to be used
      :type     - either :basic or :digest
      :user     - user name to be used
      :password - password to be used
      :realm    - realm name to authenticate in
    :timeout - request timeout in ms")]
              `(defn ~fn-name ~fn-doc [~'client #^String ~'url & {:as ~'options}]
                 (apply execute-request ~'client
                        (apply prepare-request ~method# ~'url (apply concat ~'options))
                        (apply concat *default-callbacks*)))))
          methods)))

(gen-methods :get :post :put :delete :head :options)

(defn request-stream
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  [client method #^String url body-part-callback & {:as options}]
  (apply execute-request client
         (apply prepare-request method url (apply concat options))
         (apply concat (merge *default-callbacks* {:part body-part-callback}))))

(defn stream-seq
  "Creates potentially infinite lazy sequence of Http Stream."
  [client method #^String url & {:as options}]
  (let [que (LinkedBlockingQueue.)]
    (apply execute-request client
           (apply prepare-request method url (apply concat options))
           (apply concat (merge
                          *default-callbacks*
                          {:part (fn [_ baos]
                                   (.put que baos)
                                   [que :continue])
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
  (let [b (safe-get :body resp)]
    (if (instance? LinkedBlockingQueue b)
      ((fn gen-next []
         (lazy-seq
          (let [v (.take b)]
            (when-not (= ::done v)
              (cons v (gen-next)))))))
      b)))

(defn- convert [#^ByteArrayOutputStream baos enc]
  (.toString baos enc))

(defn- convert-body [body enc]
  (if (seq? body)
    (map #(convert % enc) body)
    (convert body enc)))

(defn string
  "Converts response to string.
  Or converts body taking encoding from response."
  ([resp]
     (when-let [body (body resp)]
       (convert-body body (or (get-encoding (headers resp)) duck/*default-encoding*))))
  ([headers body]
     (convert-body body (or (get-encoding headers) duck/*default-encoding*))))

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
  ([client] (.close client)))
