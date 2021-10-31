;;; ## websocket.clj -- Asynchronous HTTP Client Websocket support for Clojure

;;; Copyright 2015 Chris Hapgood

;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at

;;; http://www.apache.org/licenses/LICENSE-2.0

;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.
(ns http.async.client.websocket
  "Asynchronous HTTP Client Websocket Extensions- Clojure"
  {:author "Chris Hapgood"}
  (:refer-clojure :exclude [await send])
  (:import (java.io ByteArrayOutputStream)
           (com.ning.http.client HttpResponseBodyPart)
           (com.ning.http.client.ws WebSocket
                                    WebSocketUpgradeHandler$Builder
                                    WebSocketListener
                                    WebSocketByteListener WebSocketByteFragmentListener
                                    WebSocketTextListener WebSocketTextFragmentListener
                                    WebSocketCloseCodeReasonListener)
           (com.ning.http.client.providers.netty.ws NettyWebSocket)
           (com.ning.http.client.providers.netty NettyAsyncHttpProviderConfig)))

(defprotocol IWebSocket
  (-sendText [this text])
  (-sendByte [this byte]))

(extend-protocol IWebSocket
  NettyWebSocket
  (-sendText [ws text]
    (.sendMessage ws text))
  (-sendByte [ws byte]
    (.sendMessage ws byte)))

(defn send
  "Send message via WebSocket."
  [ws & {text :text
         byte :byte}]
  (when (satisfies? IWebSocket ws)
    (if text
      (-sendText ws text)
      (-sendByte ws byte))))

(defn- create-text-listener
  [ws cb open-cb close-cb error-cb]
  (reify
    WebSocketCloseCodeReasonListener
    (onClose [_ ws* code reason]
      (when close-cb (close-cb ws* code reason))
      (reset! ws nil))

    WebSocketListener
    (^{:tag void} onOpen [_ #^WebSocket soc]
     (reset! ws soc)
     (when open-cb (open-cb soc)))
    (^{:tag void} onClose [_ #^WebSocket soc])
    (^{:tag void} onError [_ #^Throwable t]
     (reset! ws nil)
     (when error-cb (error-cb @ws t)))

    WebSocketTextListener
    (^{:tag void} onMessage [_ #^String s]
     (cb @ws s))
    WebSocketTextFragmentListener
    (^{:tag void} onFragment [_ #^HttpResponseBodyPart part])))

(defn- create-byte-listener
  [ws cb open-cb close-cb error-cb]
  (reify
    WebSocketCloseCodeReasonListener
    (onClose [_ ws* code reason]
      (when close-cb (close-cb ws* code reason))
      (reset! ws nil))

    WebSocketListener
    (^{:tag void} onOpen [_ #^WebSocket soc]
     (reset! ws soc)
     (when open-cb (open-cb soc)))
    (^{:tag void} onClose [_ #^WebSocket soc])
    (^{:tag void} onError [_ #^Throwable t]
     (reset! ws nil)
     (when error-cb (error-cb @ws t)))

    WebSocketByteListener
    (^{:tag void} onMessage [_ #^bytes b]
     (cb @ws b))
    WebSocketByteFragmentListener
    (^{:tag void} onFragment [_ #^HttpResponseBodyPart part])))

(defn upgrade-handler
  "Creates a WebSocketUpgradeHandler"
  {:tag WebSocket}
  [& {text-cb  :text
      byte-cb  :byte
      open-cb  :open
      close-cb :close
      error-cb :error}]
  (let [b (WebSocketUpgradeHandler$Builder.)
        ws (atom nil)]
    (when text-cb (.addWebSocketListener b (create-text-listener ws text-cb open-cb close-cb error-cb)))
    (when byte-cb (.addWebSocketListener b (create-byte-listener ws byte-cb open-cb close-cb error-cb)))
    (.build b)))
