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

(ns async.http.client.test
  "Testing of ahc-clj"
  #^{:author "Hubert Iwaniuk <neotyk@kungfoo.pl>"}
  (:use clojure.test
        async.http.client
        async.http.client.request
        [clojure.stacktrace :only [print-stack-trace]]
        [clojure.contrib.str-utils2 :only [split]])
  (:import (org.apache.log4j ConsoleAppender Level Logger PatternLayout)
           (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler AbstractHandler)
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
           (doseq [[k [v]] (.getParameterMap hReq)]
                  (.addHeader hResp k v))
           (when-let [q (.getQueryString hReq)]
            (doseq [p (split q #"\&")]
              (let [[k v] (split p #"=")]
                (.setHeader hResp k v))))
           (doto hResp
             (.setContentType "text/plain;charset=utf-8")
             (.setStatus 200))
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
	   (prepare-get "http://localhost:8080/")
	   {:status (fn [_ st] (do (deliver status# st) :abort))
            :part body-collect
            :completed body-completed
            :headers headers-collect})
	status @status#]
    (is (= (status :code) 200))
    (is (= (status :msg) "OK"))
    (is (= (status :protocol) "HTTP/1.1"))
    (is (= (status :major) 1))
    (is (= (status :minor) 1))))

(deftest test-receive-headers
  (let [headers# (promise)
        _ (execute-request
           (prepare-get "http://localhost:8123/")
           {:status status-collect
            :part body-collect
            :completed body-completed
            :headers (fn [_ hds] (do (deliver headers# hds) :abort))})
        headers @headers#]
    (is (= (headers :test-header) "test-value"))))

(deftest test-send-headers
  (let [resp (GET "http://localhost:8123/" {:headers {:a 1 :b 2}})
        headers (@resp :headers)]
    (if (contains? @resp :error)
      (print-stack-trace (:error @resp)))
    (is (not (contains? (keys @resp) :error)))
    (is (not (empty? headers)))
    (are (= (headers :a) 1)
         (= (headers :b) 2))))

(deftest test-body
  (let [resp (GET "http://localhost:8080/")
        headers (@resp :headers)
        body (@resp :body)]
    (is (= "<h1>Hello WWW!</h1>\n" (apply str (map char body))))
    (is (not (empty? body)))
    (if (contains? headers :content-length)
      (is (= (count body) (:content-length headers))))))

(deftest test-query-params
  (let [resp (GET "http://localhost:8123/" {:query {:a 3 :b 4}})
        headers (@resp :headers)]
    (is (not (empty? headers)))
    (are [x y] (= x (str y))
         (headers :a) 3
         (headers :b) 4)))

(deftest test-get-params-not-allowed
  (is (thrown?
       IllegalArgumentException
       (GET "http://localhost:8123/" {:param {:a 5 :b 6}}))))

(deftest test-post-params
  (let [resp (POST "http://localhost:8123/" nil {:param {:a 5 :b 6}})
        headers (:headers @resp)]
    (is (not (empty? headers)))
    (are [x y] (= x (str y))
         (:a headers) 5
         (headers :b) 6)))
