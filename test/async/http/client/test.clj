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

(ns
    #^{:author "Hubert Iwaniuk"
       :doc "Testing of ahc-clj"}
  async.http.client.test
  (:use clojure.test
        async.http.client
        async.http.client.request
        [clojure.stacktrace :only [print-stack-trace]]
        [clojure.contrib.str-utils2 :only [split]]
        [clojure.java.io :only [input-stream]])
  (:import (org.apache.log4j ConsoleAppender Level Logger PatternLayout)
           (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.continuation Continuation ContinuationSupport)
           (javax.servlet.http HttpServletRequest HttpServletResponse)))

(def default-handler
     (proxy [AbstractHandler] []
       (handle [target #^Request req #^HttpServletRequest hReq #^HttpServletResponse hResp]
         (do
           (.setHeader hResp "test-header" "test-value")
           (let [hdrs (enumeration-seq (.getHeaderNames hReq))]
             (doseq [k hdrs :when (not (contains? #{"Server"
                                                    "Connection"
                                                    "Content-Length"
                                                    "Host"
                                                    "User-Agent"
                                                    "Content-Type"} k))]
               (.setHeader hResp k (.getHeader hReq k))))
           (.setContentType hResp "text/plain;charset=utf-8")
           (.setStatus hResp 200)
                                        ; process params
           (condp = target
               "/body" (.write (.getWriter hResp) "Remember to checkout #clojure@freenode")
               "/body-str" (when-let [line (.readLine (.getReader hReq))]
                             (.write (.getWriter hResp) line))
               "/put" (.setHeader hResp "Method" (.getMethod hReq))
               "/delete" (.setHeader hResp "Method" (.getMethod hReq))
               "/head" (.setHeader hResp "Method" (.getMethod hReq))
               "/options" (.setHeader hResp "Method" (.getMethod hReq))
               "/stream" (do
                           (let [cont (ContinuationSupport/getContinuation hReq)
                                 writer (.getWriter hResp)
                                 prom (promise)]
                             (.suspend cont)
                             (future
                              (Thread/sleep 100)
                              (doto writer
                                (.write "part1")
                                (.flush)))
                             (future
                              (Thread/sleep 200)
                              (doto writer
                                (.write "part2")
                                (.flush))
                              (deliver prom true))
                             (future
                              (if @prom
                                (.complete cont)))))
               (doseq [n (enumeration-seq (.getParameterNames hReq))]
                 (doseq [v (.getParameterValues hReq n)]
                   (.addHeader hResp n v))))
           (when-let [q (.getQueryString hReq)]
             (doseq [p (split q #"\&")]
               (let [[k v] (split p #"=")]
                 (.setHeader hResp k v))))
           (.setHandled req true)))))

(defn- start-jetty
  ([handler]
     (start-jetty handler {:port 8123}))
  ([handler {port :port :as opts :or {:port 8123}}]
     (let [srv (Server. port)]
       (doto srv
         (.setHandler handler)
         (.start))
       srv)))

(defn- once-fixture [f]
  "Configures Logger before test here are executed, and closes AHC after tests are done."
  (doto (Logger/getRootLogger)
    (.setLevel Level/WARN)
    (.addAppender (ConsoleAppender. (PatternLayout. PatternLayout/TTCC_CONVERSION_PATTERN))))
  (def srv (start-jetty default-handler))
  (try (f)
       (finally
        (do
          (.close ahc)
          (.stop srv)))))

(use-fixtures :once once-fixture)

(deftest test-status
  (let [status# (promise)
	_ (execute-request
	   (prepare-request :get "http://localhost:8123/")
	   {:status (fn [_ st] (do (deliver status# st) :abort))
            :part body-collect
            :completed body-completed
            :headers headers-collect
            :error error-collect})
	status @status#]
    (are [k v] (= (k status) v)
         :code 200
         :msg "OK"
         :protocol "HTTP/1.1"
         :major 1
         :minor 1)))

(deftest test-receive-headers
  (let [headers# (promise)
        _ (execute-request
           (prepare-request :get "http://localhost:8123/")
           {:status status-collect
            :part body-collect
            :completed body-completed
            :headers (fn [_ hds] (do (deliver headers# hds) :abort))
            :error error-collect})
        headers @headers#]
    (is (= (headers :test-header) "test-value"))))

(deftest test-send-headers
  (let [resp (GET "http://localhost:8123/" {:headers {:a 1 :b 2}})
        headers (@resp :headers)]
    (if (contains? @resp :error)
      (print-stack-trace (:error @resp)))
    (is (not (contains? (keys @resp) :error)))
    (is (not (empty? headers)))
    (are [k v] (= (k headers) (str v))
         :a 1
         :b 2)))

(deftest test-body
  (let [resp (GET "http://localhost:8123/body")
        headers (@resp :headers)
        body (@resp :body)]
    (is (not (empty? body)))
    (is (= "Remember to checkout #clojure@freenode" (apply str (map char body))))
    (if (contains? headers :content-length)
      (is (= (count body) (:content-length headers))))))

(deftest test-query-params
  (let [resp (GET "http://localhost:8123/" {:query {:a 3 :b 4}})
        headers (@resp :headers)]
    (is (not (empty? headers)))
    (are [x y] (= (x headers) (str y))
         :a 3
         :b 4)))

(deftest test-get-params-not-allowed
  (is (thrown?
       IllegalArgumentException
       (GET "http://localhost:8123/" {:param {:a 5 :b 6}}))))

(deftest test-post-params
  (let [resp (POST "http://localhost:8123/" nil {:param {:a 5 :b 6}})
        headers (:headers @resp)]
    (is (not (empty? headers)))
    (are [x y] (= (x headers) (str y))
         :a 5
         :b 6)))

(deftest test-post-string-body
  (let [resp (POST "http://localhost:8123/body-str" "TestBody" nil)
        headers (:headers @resp)
        body (:body @resp)]
    (are [x] (not (empty? x))
         headers
         body)
    (is (= "TestBody" (apply str (map char body))))))

(deftest test-post-map-body
  (let [resp (POST "http://localhost:8123/" {:u "user" :p "s3cr3t"})
        headers (:headers @resp)]
    (is (not (empty? headers)))
    (are [x y] (= x (y headers))
         "user" :u
         "s3cr3t" :p)))

(deftest test-post-input-stream-body
  (let [resp (POST "http://localhost:8123/body-str" (input-stream (.getBytes "TestContent" "UTF-8")))
        headers (:headers @resp)
        body (:body @resp)]
    (are [x] (not (empty? x))
         headers
         body)
    (is (= "TestContent" (apply str (map char body))))))

(deftest test-put
  (let [resp (PUT "http://localhost:8123/put" "TestContent")
        status (:status @resp)
        headers (:headers @resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "PUT" (:method headers)))))

(deftest test-delete
  (let [resp (DELETE "http://localhost:8123/delete")
        status (:status @resp)
        headers (:headers @resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "DELETE" (:method headers)))))

(deftest test-head
  (let [resp (HEAD "http://localhost:8123/head")
        status (:status @resp)
        headers (:headers @resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "HEAD" (:method headers)))))

(deftest test-options
  (let [resp (OPTIONS "http://localhost:8123/options")
        status (:status @resp)
        headers (:headers @resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "OPTIONS" (:method headers)))))

(deftest test-stream
  (let [stream (ref #{})
        consume-stream (fn [_ bytes]
                         (if (not (empty? bytes))
                           (let [s (apply str (map char bytes))]
                             (dosync (alter stream conj s)))
                           (println "Received empty body part instead of stream.")))
        resp (STREAM :get "http://localhost:8123/stream" consume-stream)
        status (:status @resp)]
    (are [x] (not (empty? x))
         status
         @stream)
    (is (= 200 (:code status)))
    (doseq [s @stream]
      (let [part s]
        (is (contains? #{"part1" "part2"} part))))))

(deftest test-get-stream
  (let [resp (GET "http://localhost:8123/stream")
        body (@resp :body)]
    (is (= "part1part2" (apply str (map char body))))))

(deftest test-stream-seq
  (let [resp (STREAM-SEQ :get "http://localhost:8123/stream")
        status-received @(:status-received @resp)
        status (:status @resp)
        headers-received @(:headers-received @resp)
        headers (:headers @resp)
        body-started @(:body-started @resp)
        body (:body @resp)
        body-finished @(:body-finished @resp)]
    (are [e p] (= e p)
         true status-received
         200 (:code status)
         true headers-received
         "test-value" (:test-header headers)
         true body-started
         2 (count body)
         true body-finished)
    (doseq [s body]
      (is (or (= "part1" s) (= "part2" s))))))

;(require 'clojure.contrib.json)
;(deftest test-stream-proto
;  (let [stream-queue (java.util.concurrent.LinkedBlockingQueue. 10)
;        stream-seq ((fn thisfn []
;                      (lazy-seq
;                       (let [v (.take stream-queue)]
;                         ;(println "took: " v)
;                         (when-not (= ::done v)
;                           (cons
;                            v
;                            (thisfn)))))))
;        create-lazy-seq (fn [state bytes]
;                          (if (not (empty? bytes))
;                           (let [v (apply str (map char bytes))]
;                             ;(println "putting: " v)
;                             (.put stream-queue v))))
;        lazy-seq-completed (fn [state]
;                             (println "putting: nil")
;                             (.put stream-queue ::done))
;        resp (execute-request
;              (prepare-request :get "http://stream.twitter.com/1/statuses/sample.json"
;                               {:headers {:authorization "Basic YWhjY2xqOmFoY2NsajEx"}})
;              {:status status-collect
;               :headers headers-collect
;               :part create-lazy-seq
;               :completed lazy-seq-completed
;               :error error-collect})]
;    (doseq [s (take 2 stream-seq)] (println (select-keys (clojure.contrib.json/read-json s) [:text ;:screen_name])))
;    (is (= "part1" (first stream-seq)))
;    (is (= "part2" (nth stream-seq 1)))
;    (is (= nil (nnext stream-seq)))))
