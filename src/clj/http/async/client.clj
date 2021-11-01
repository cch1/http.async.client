;;; ## client.clj -- Asynchronous HTTP Client for Clojure

;;; Copyright 2011 Hubert Iwaniuk

;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at

;;; http://www.apache.org/licenses/LICENSE-2.0

;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.
(ns http.async.client
  "Asynchronous HTTP Client - Clojure"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [await send])
  (:require [http.async.client
             [request :refer :all]
             [headers :refer :all]
             [util :refer :all]
             [websocket :as ws]]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayOutputStream)
           (java.util.concurrent LinkedBlockingQueue)
           (org.asynchttpclient AsyncHttpClient DefaultAsyncHttpClientConfig$Builder Request DefaultAsyncHttpClient)
           (org.asynchttpclient.ws WebSocket WebSocketUpgradeHandler)
           (javax.net.ssl SSLContext)
           (org.asynchttpclient.netty.ssl JsseSslEngineFactory DefaultSslEngineFactory)
           (io.netty.handler.ssl SslContext)))

(defn- set-ssl-context
  "This fn exists just to figure out whether the `ssl-context` we're setting
  is an instance of javax.net.ssl.SSLContext or
  io.netty.handler.ssl.SslContext. In older versions of the java lib we're
  wrapping, javax.net.ssl.SSLContext was always used. But recent versions
  have to pass a different SSLEngineFactory to support the former while the
  latter is the new default."
  [#^DefaultAsyncHttpClientConfig$Builder client-config-builder ssl-context]
  (cond
    (instance? SSLContext ssl-context)
    (.setSslEngineFactory client-config-builder
                          (JsseSslEngineFactory. ssl-context))

    (instance? SslContext ssl-context)
    (.setSslContext client-config-builder ssl-context)

    :else
    (throw
      (IllegalArgumentException.
        (str "ssl-context must be an instance of either "
             "javax.net.ssl.SSLContext or io.netty.handler.ssl.SslContext; "
             "got an instance of " (class ssl-context) " instead")))))


;; # Client Lifecycle

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
  - :read-timeout :: read timeout in ms
  - :request-timeout :: request timeout in ms
  - :user-agent :: User-Agent branding string
  - :thread-factory :: Provide your own ThreadFactory for callbacks to be executed with
  - :ssl-context :: provide your own SSL Context
  - :websocket :: map with websocket-config
      :max-frame-size                      - set websocket max-frame size
      :max-buffer-size                     - set websdocket max-buffer size
      :aggregate-websocket-frame-fragments - set aggregate websocket frame fragments
      :enable-compression                  - set enable compression"
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
             read-timeout
             request-timeout
             user-agent
             thread-factory
             ssl-context
             websocket]}]
  (DefaultAsyncHttpClient.
   (.build
    (let [b (DefaultAsyncHttpClientConfig$Builder.)]
      (when-not (nil? compression-enabled) (.setCompressionEnforced b compression-enabled))
      (when connection-timeout (.setConnectTimeout b connection-timeout))
      (when-not (nil? follow-redirects) (.setFollowRedirect b follow-redirects))
      (when idle-in-pool-timeout (.setPooledConnectionIdleTimeout b idle-in-pool-timeout))
      (when-not (nil? keep-alive) (.setKeepAlive b keep-alive))
      (when max-conns-per-host (.setMaxConnectionsPerHost b max-conns-per-host))
      (when max-conns-total (.setMaxConnections b max-conns-total))
      (when max-redirects (.setMaxRedirects b max-redirects))
      (when thread-factory (.setThreadFactory b thread-factory))
      (when proxy
        (set-proxy proxy b))
      (when auth
        (set-realm auth b))
      (when websocket
        (set-websocket-config websocket b))
      (when read-timeout (.setReadTimeout b read-timeout))
      (when request-timeout (.setRequestTimeout b request-timeout))
      (.setUserAgent b (if user-agent user-agent *user-agent*))
      (when-not (nil? ssl-context) (set-ssl-context b ssl-context))
      b))))

(defmacro ^{:private true} gen-methods [& methods]
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
    :query   - map of query parameters, if value is vector than multiple values
               will be send as n=v1&n=v2
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
                     (execute-request ~'client
                                      (apply prepare-request ~method# ~'url
                                             (apply concat ~'options))))))
              methods)))

(gen-methods :get :post :put :delete :head :options :patch)

(defn request-stream
  "Consumes stream from given url.
  method - HTTP method to be used (:get, :post, ...)
  url - URL to set request to
  body-part-callback - callback that takes status (ref {}) of request
                       and received body part as vector of bytes
  options - are optional and can contain :headers, :param, and :query (see prepare-request)."
  [client method #^String url body-part-callback & {:as options}]
  (execute-request client
                   (apply prepare-request method url (apply concat options))
                   :part body-part-callback))

(defn stream-seq
  "Creates potentially infinite lazy sequence of Http Stream."
  [client method #^String url & {:as options}]
  (let [que (LinkedBlockingQueue.)]
    (execute-request client
                     (apply prepare-request method url (apply concat options))
                     :part (fn [_ baos]
                             (.put que baos)
                             [que :continue])
                     :completed (fn [_] (.put que ::done))
                     :error (fn [_ t]
                              (.put que ::done)
                              t))))

(defn failed?
  "Checks if request failed."
  [resp]
  (realized? (:error resp)))

(defn done?
  "Checks if request is finished already (response receiving finished)."
  [resp]
  (realized? (:done resp)))

(defn- safe-get
  [k r]
  (when-not (failed? r)
    @(k r)))

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
          (let [v (.take ^LinkedBlockingQueue b)]
            (if (= ::done v)
              (do
                (.put ^LinkedBlockingQueue b ::done)
                nil)
              (cons v (gen-next)))))))
      b)))

(defn- convert [#^ByteArrayOutputStream baos enc]
  (.toString baos #^String enc))

(defn- convert-body [body enc]
  (if (seq? body)
    (map #(convert % enc) body)
    (convert body enc)))

(def ^:private ^:dynamic *default-encoding* "UTF-8")

(defn string
  "Converts response to string.
  Or converts body taking encoding from response."
  ([resp]
   (when-let [body (body resp)]
     (convert-body body (or (get-encoding (headers resp)) *default-encoding*))))
  ([headers body]
   (convert-body body (or (get-encoding headers) *default-encoding*))))

(defn cookies
  "Gets cookies from response."
  [resp]
  (create-cookies (headers resp)))

(defn content-type
  "Gets content type from response."
  [resp]
  (when-let [hs (headers resp)]
    (:content-type hs)))

(defn status
  "Gets status if status was delivered."
  [resp]
  (safe-get :status resp))

(defn redirect?
  "Checks if response is redirect."
  [resp]
  (when-let [st (status resp)]
    (<= 300 (:code st) 399)))

(defn location
  "Retrieves location of redirect."
  [resp]
  (when-let [hs (headers resp)]
    (:location hs)))

(defn error
  "Returns Throwable if request processing failed."
  [resp]
  (when (failed? resp)
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

(defn url
  "Gets URL from response"
  [resp]
  (:url resp))

(defn uri
  "Get the request URI from the response"
  [resp]
  (.toJavaNetURI (.getUri ^Request (:req resp))))

(defn send
  "Send message via WebSocket."
  [ws & {text :text
         byte :byte}]
  (when (satisfies? ws/IWebSocket ws)
    (if text
      (ws/-sendText ws text)
      (ws/-sendByte ws byte))))

(defn websocket
  "Opens WebSocket connection."
  {:tag WebSocket}
  [^AsyncHttpClient client #^String url & options]
  (let [^WebSocketUpgradeHandler wsugh (apply ws/upgrade-handler options)
        ^Request req (apply prepare-request :get url options)]
    (.get (.executeRequest client req wsugh))))

;; closing

(defn close-websocket [ws]
  (.sendCloseFrame ws))

(defprotocol IClosable
  (-close [this])
  (-open? [this]))

(extend-protocol IClosable
  AsyncHttpClient
  (-close [client] (.close client))
  (-open? [client] (not (.isClosed client))))

  ;NettyWebSocket
  ;(-close [soc] (.sendCloseFrame soc))
  ;(-open? [soc] (.isOpen soc)))

(defn close
  "Closes client."
  [client]
  (when (satisfies? IClosable client)
    (let [result (-close client)]
      (log/debug "Closed client: " client)
      result)))

(defn open?
  "Checks if client is open."
  [client]
  (when (satisfies? IClosable client)
    (-open? client)))
