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
  (:import (org.asynchttpclient.ws WebSocket
                                   WebSocketUpgradeHandler$Builder
                                   WebSocketListener)
           (org.asynchttpclient.netty.ws NettyWebSocket)))

(defprotocol IWebSocket
  (-sendText [this text])
  (-sendByte [this byte]))

(extend-protocol IWebSocket
  NettyWebSocket
  (-sendText [ws text]
    (.sendTextFrame ws text))
  (-sendByte [ws byte]
    (.sendBinaryFrame ws byte)))

(defn send
 "Send message via WebSocket."
 [ws & {text :text
        byte :byte}]
 (when (satisfies? IWebSocket ws)
   (if text
     (-sendText ws text)
     (-sendByte ws byte))))

(defn- create-listener
  [ws text-cb byte-cb open-cb close-cb error-cb]
  (reify
    WebSocketListener
    (^{:tag void} onOpen [_ #^WebSocket soc]
     (reset! ws soc)
     (when open-cb (open-cb soc)))
    (onClose [_ ws* code reason]
      (when close-cb (close-cb ws* code reason))
      (reset! ws nil))
    (^{:tag void} onError [_ #^Throwable t]
     (reset! ws nil)
     (when error-cb (error-cb @ws t)))
    (onTextFrame [_ s _ _]
     (when text-cb (text-cb @ws s)))
    (onBinaryFrame [_ b _ _]
     (when byte-cb (byte-cb @ws b)))))

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
    (.addWebSocketListener b (create-listener ws text-cb byte-cb open-cb close-cb error-cb))
    (.build b)))
