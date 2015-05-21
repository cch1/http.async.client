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

(ns http.async.client.test
  "Testing of http.async.client"
  {:author "Hubert Iwaniuk"}
  (:refer-clojure :exclude [await send])
  (:require [http.async.client :refer :all]
            [http.async.client
             [request :refer :all]
             [util :refer :all]]
            [clojure
             [test :refer :all]
             [stacktrace :refer [print-stack-trace]]
             [string :refer [split]]]
            [clojure.java.io :refer [input-stream]])
  (:import (com.ning.http.client AsyncHttpClient)
           (org.apache.log4j ConsoleAppender Level Logger PatternLayout)
           (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.continuation Continuation ContinuationSupport)
           (org.eclipse.jetty.http.security Constraint)
           (org.eclipse.jetty.security ConstraintMapping ConstraintSecurityHandler
                                            HashLoginService LoginService)
           (org.eclipse.jetty.security.authentication BasicAuthenticator)
           (javax.servlet.http HttpServletRequest HttpServletResponse Cookie)
           (java.io ByteArrayOutputStream
                    File
                    IOException)
           (java.net ServerSocket
                     ConnectException)
           (java.nio.channels UnresolvedAddressException)
           (java.util.concurrent TimeoutException)))
(set! *warn-on-reflection* true)

(def ^:dynamic *client* nil)
(def ^:dynamic *server* nil)
(def ^:private ^:dynamic *default-encoding* "UTF-8")


;; test suite setup
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
                                                    "Content-Type"
                                                    "Cookie"} k))]
               (.setHeader hResp k (.getHeader hReq k))))
           (.setContentType hResp "text/plain;charset=utf-8")
           (.setStatus hResp 200)
                                        ; process params
           (condp = target
             "/body" (.write (.getWriter hResp) "Remember to checkout #clojure@freenode")
             "/body-str" (when-let [line (.readLine (.getReader hReq))]
                           (.write (.getWriter hResp) line))
             "/body-multi" (let [#^String body (slurp (.getReader hReq))]
                             (.write (.getWriter hResp) body))
             "/put" (.setHeader hResp "Method" (.getMethod hReq))
             "/post" (do
                       (.setHeader hResp "Method" (.getMethod hReq))
                       (when-let [line (.readLine (.getReader hReq))]
                         (.write (.getWriter hResp) line)))
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
             "/issue-1" (let [writer (.getWriter hResp)]
                          (doto writer
                            (.write "глава")
                            (.flush)))
             "/proxy-req" (.setHeader hResp "Target" (.. req (getUri) (toString)))
             "/cookie" (do
                         (.addCookie hResp (Cookie. "foo" "bar"))
                         (doseq [c (.getCookies hReq)]
                           (.addCookie hResp c)))
             "/branding" (.setHeader hResp "X-User-Agent" (.getHeader hReq "User-Agent"))
             "/basic-auth" (let [auth (.getHeader hReq "Authorization")]
                             (.setStatus
                              hResp
                              (if (= auth "Basic YmVhc3RpZTpib3lz")
                                200
                                401)))
             "/preemptive-auth" (let [auth (.getHeader hReq "Authorization")]
                                  (.setStatus
                                   hResp
                                   (if (= auth "Basic YmVhc3RpZTpib3lz")
                                     200
                                     401)))
             "/timeout" (Thread/sleep 2000)
             "/empty" (.setHeader hResp "Nothing" "Yep")
             "/multi-query" (.setHeader hResp "query" (.getQueryString hReq))
             "/redirect" (.sendRedirect hResp "here")
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
     (let [srv (Server. port)
           loginSrv (HashLoginService. "MyRealm" "test-resources/realm.properties")
           constraint (Constraint.)
           mapping (ConstraintMapping.)
           security (ConstraintSecurityHandler.)]

       (.addBean srv loginSrv)
       (doto constraint
         (.setName Constraint/__BASIC_AUTH)
         (.setRoles (into-array #{"user"}))
         (.setAuthenticate true))
       (doto mapping
         (.setConstraint constraint)
         (.setPathSpec "/basic-auth"))
       (doto security
         (.setConstraintMappings (into-array #{mapping}) #{"user"})
         (.setAuthenticator (BasicAuthenticator.))
         (.setLoginService loginSrv)
         (.setStrict false)
         (.setHandler handler))
       (doto srv
         (.setHandler security)
         (.start))
       srv)))

(defn- once-fixture [f]
  "Configures Logger before test here are executed, and closes AHC after tests are done."
  (binding [*server* (start-jetty default-handler)]
    (try (f)
         (finally
           (.stop ^Server *server*)))))

(defn- each-fixture
  [f]
  (binding [*client* (create-client)]
    (try (f)
         (finally
           (.close ^AsyncHttpClient *client*)))))

(use-fixtures :once once-fixture)
(use-fixtures :each each-fixture)

;; testing
(deftest test-status
  (let [status# (promise)
	_ (execute-request *client*
                           (prepare-request :get "http://localhost:8123/")
                           :status (fn [_ st]
                                     (deliver status# st)
                                     [st :abort]))
	status @status#]
    (are [k v] (= (k status) v)
         :code 200
         :msg "OK"
         :protocol "HTTP/1.1"
         :major 1
         :minor 1)))

(deftest test-receive-headers
  (let [headers# (promise)
        _ (execute-request *client*
                           (prepare-request :get "http://localhost:8123/")
                           :headers (fn [_ hds]
                                      (deliver headers# hds)
                                      [hds :abort]))
        headers @headers#]
    (is (= (:test-header headers) "test-value"))
    (is (thrown? UnsupportedOperationException (.cons ^clojure.lang.APersistentMap headers '())))
    (is (thrown? UnsupportedOperationException (assoc ^clojure.lang.APersistentMap headers :a 1)))
    (is (thrown? UnsupportedOperationException (.without ^clojure.lang.APersistentMap headers :a)))))

(deftest test-status-and-header-callbacks
  (let [status# (promise)
        headers# (promise)
        _ (execute-request *client*
                           (prepare-request :get "http://localhost:8123/")
                           :status (fn [_ st]
                                     (deliver status# st)
                                     [st :continue])
                           :headers (fn [_ hds]
                                      (deliver headers# hds)
                                      [hds :abort]))
        status @status#
        headers @headers#]
    (are [k v] (= (k status) v)
         :code 200
         :msg "OK"
         :protocol "HTTP/1.1"
         :major 1
         :minor 1)
    (is (= (:test-header headers) "test-value"))))

(deftest test-body-part-callback
  (testing "callecting body callback"
    (let [parts (atom #{})
          resp (execute-request *client*
                                (prepare-request :get "http://localhost:8123/stream")
                                :part (fn [response ^ByteArrayOutputStream part]
                                        (let [p (.toString part ^String *default-encoding*)]
                                          (swap! parts conj p)
                                          [p :continue])))]
      (await resp)
      (is (contains? @parts "part1"))
      (is (contains? @parts "part2"))
      (is (= 2 (count @parts)))))
  (testing "counting body parts callback"
    (let [cnt (atom 0)
          resp (execute-request *client*
                                (prepare-request :get "http://localhost:8123/stream")
                                :part (fn [_ ^ByteArrayOutputStream p]
                                        (swap! cnt inc)
                                        [p :continue]))]
      (await resp)
      (is (= 2 @cnt)))))

(deftest test-body-completed-callback
  (testing "successful response"
    (let [finished (promise)
          resp (execute-request *client*
                                (prepare-request :get "http://localhost:8123/")
                                :completed (fn [response]
                                             (deliver finished true)))]
      (await resp)
      (is (true? (realized? finished)))
      (is (true? @finished))))
  (testing "execution time"
    (let [finished (promise)
          req (prepare-request :get "http://localhost:8123/body")
          start (System/currentTimeMillis)
          resp (execute-request *client* req
                                :completed (fn [_]
                                             (deliver finished
                                                      (- (System/currentTimeMillis) start))))]
      (is (pos? @finished))))
  (testing "failed response"
    (let [finished (promise)
          resp (execute-request *client*
                                (prepare-request :get "http://not-existing-host/")
                                :completed (fn [response]
                                             (deliver finished true)))]
      (await resp)
      (is (false? (realized? finished))))))

(deftest test-error-callback
  (let [errored (promise)
        resp (execute-request *client* (prepare-request :get "http://not-existing-host/")
                              :error (fn [_ e]
                                       (deliver errored e)))]
    (await resp)
    (is (true? (realized? errored)))
    (is (true? (instance? java.net.ConnectException @errored)))))

(deftest test-error-callback-throwing
  (let [resp (execute-request *client* (prepare-request :get "http://not-existing-host/")
                              :error (fn [_ _]
                                       (throw (Exception. "boom!"))))]
    (await resp)
    (is (done? resp))
    (is (failed? resp))
    (is (= "boom!" (.getMessage ^Exception (error resp))))))

(deftest test-send-headers
  (let [resp (GET *client* "http://localhost:8123/" :headers {:a 1 :b 2})
        headers (headers resp)]
    (if (realized? (:error resp))
      (print-stack-trace @(:error resp)))
    (is (not (realized? (:error resp))))
    (is (not (empty? headers)))
    (are [k v] (= (k headers) (str v))
         :a 1
         :b 2)))

(deftest test-body
  (let [resp (GET *client* "http://localhost:8123/body")
        headers (headers resp)
        body (body resp)]
    (is (not (nil? body)))
    (is (= "Remember to checkout #clojure@freenode" (string resp)))
    (if (contains? headers :content-length)
      (is (= (count (string resp)) (Integer/parseInt (:content-length headers)))))))

(deftest test-query-params
  (let [resp (GET *client* "http://localhost:8123/" :query {:a 3 :b 4})
        headers (headers resp)]
    (is (not (empty? headers)))
    (are [x y] (= (x headers) (str y))
         :a 3
         :b 4)))

(deftest test-query-params-multiple-values
  (let [resp (GET *client* "http://localhost:8123/multi-query" :query {:multi [3 4]})
        headers (headers resp)]
    (is (not (empty? headers)))
    (is (= "multi=3&multi=4" (:query headers)))))

;; TODO: uncomment this test once AHC throws exception again on body
;; with GET
;; (deftest test-get-params-not-allowed
;;   (is (thrown?
;;        IllegalArgumentException
;;        (GET *client* "http://localhost:8123/" :body "Boo!"))))

(deftest test-post-no-body
  (let [resp (POST *client* "http://localhost:8123/post")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "POST" (:method headers)))
    (is (done? (await resp)))
    (is (nil? (string resp)))))

(deftest test-post-params
  (let [resp (POST *client* "http://localhost:8123/" :body {:a 5 :b 6})
        headers (headers resp)]
    (is (not (empty? headers)))
    (are [x y] (= (x headers) (str y))
         :a 5
         :b 6)))

(deftest test-post-string-body
  (let [resp (POST *client* "http://localhost:8123/body-str" :body "TestBody  Encoded?")
        headers (headers resp)]
    (is (not (empty? headers)))
    (is (= "TestBody  Encoded?" (string resp)))))

(deftest test-post-string-body-content-type-encoded
  (let [resp (POST *client* "http://localhost:8123/body-str"
                   :headers {:content-type "application/x-www-form-urlencoded"}
                   :body "Encode this & string?")
        headers (headers resp)]
    (is (not (empty? headers)))
    (is (= "Encode+this+%26+string%3F" (string resp)))))

(deftest test-post-map-body
  (let [resp (POST *client* "http://localhost:8123/"
                   :body {:u "user" :p "s3cr3t"})
        headers (headers resp)]
    (is (not (empty? headers)))
    (are [x y] (= x (y headers))
         "user" :u
         "s3cr3t" :p)))

(deftest test-post-input-stream-body
  (let [resp (POST *client* "http://localhost:8123/body-str"
                   :body (input-stream (.getBytes "TestContent" "UTF-8")))
        headers (headers resp)]
    (is (not (empty? headers)))
    (is (= "TestContent" (string resp)))))

(deftest test-post-file-body
  (let [resp (POST *client* "http://localhost:8123/body-str"
                   :body (File. "test-resources/test.txt"))]
    (is (false? (empty? (headers resp))))
    (is (= "TestContent" (string resp)))))

(deftest test-post-multipart
  (testing "String multipart part"
    (let [resp (POST *client* "http://localhost:8123/body-multi"
                     :body [{:type  :string
                             :name  "test-name"
                             :value "test-value"}])]
      (is (false? (empty? (headers resp))))
      (let [#^String s (string resp)]
        (is (true? (.startsWith s "--")))
        (are [v] #(.contains s %)
             "test-name" "test-value"))))
  (testing "File multipart part"
    (let [resp (POST *client* "http://localhost:8123/body-multi"
                     :body [{:type      :file
                             :name      "test-name"
                             :file      (File. "test-resources/test.txt")
                             :mime-type "text/plain"
                             :charset   "UTF-8"}])]
      (is (false? (empty? (headers resp))))
      (let [#^String s (string resp)]
        (is (true? (.startsWith s "--")))
        (are [v] #(.contains s %)
             "test-name" "TestContent"))))
  (testing "Byte array multipart part"
    (let [resp (POST *client* "http://localhost:8123/body-multi"
                     :body [{:type      :bytearray
                             :name      "test-name"
                             :file-name "test-file-name"
                             :data       (.getBytes "test-content" "UTF-8")
                             :mime-type  "text/plain"
                             :charset    "UTF-8"}])]
      (is (false? (empty? (headers resp))))
      (let [#^String s (string resp)]
        (is (true? (.startsWith s "--")))
        (are [v] #(.contains s %)
             "test-name" "test-file-name" "test-content"))))
  (testing "Multiple multipart parts"
    (let [resp (POST *client* "http://localhost:8123/body-multi"
                     :body [{:type  :string
                             :name  "test-str-name"
                             :value "test-str-value"}
                            {:type      :file
                             :name      "test-file-name"
                             :file      (File. "test-resources/test.txt")
                             :mime-type "text/plain"
                             :charset   "UTF-8"}
                            {:type      :bytearray
                             :name      "test-ba-name"
                             :file-name "test-ba-file-name"
                             :data       (.getBytes "test-ba-content" "UTF-8")
                             :mime-type  "text/plain"
                             :charset    "UTF-8"}])]
      (is (false? (empty? (headers resp))))
      (let [#^String s (string resp)]
        (is (true? (.startsWith s "--")))
        (are [v] #(.contains s %)
             "test-str-name" "test-str-value"
             "test-file-name" "TestContent"
             "test-ba-name" "test-ba-file-name" "test-ba-content")))))

(deftest test-put
  (let [resp (PUT *client* "http://localhost:8123/put" :body "TestContent")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "PUT" (:method headers)))
    (is (done? (await resp)))
    (is (nil? (string resp)))))

(deftest test-put-no-body
  (let [resp (PUT *client* "http://localhost:8123/put")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "PUT" (:method headers)))))

(deftest test-delete
  (let [resp (DELETE *client* "http://localhost:8123/delete")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "DELETE" (:method headers)))))

(deftest test-head
  (let [resp (HEAD *client* "http://localhost:8123/head")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "HEAD" (:method headers)))))

(deftest test-options
  (let [resp (OPTIONS *client* "http://localhost:8123/options")
        status (status resp)
        headers (headers resp)]
    (are [x] (not (empty? x))
         status
         headers)
    (is (= 200 (:code status)))
    (is (= "OPTIONS" (:method headers)))))

(deftest test-stream
  (let [stream (ref #{})
        resp (request-stream *client* :get "http://localhost:8123/stream"
                             (fn [_ ^ByteArrayOutputStream baos]
                               (dosync (alter stream conj (.toString baos ^String *default-encoding*)))
                               [baos :continue]))
        status (status resp)]
    (await resp)
    (are [x] (not (empty? x))
         status
         @stream)
    (is (= 200 (:code status)))
    (doseq [s @stream]
      (let [part s]
        (is (contains? #{"part1" "part2"} part))))))

(deftest test-get-stream
  (let [resp (GET *client* "http://localhost:8123/stream")]
    (await resp)
    (is (= "part1part2" (string resp)))))

(deftest test-stream-seq
  (testing "Simple stream."
    (let [resp (stream-seq *client* :get "http://localhost:8123/stream")
          status (status resp)
          headers (headers resp)
          body (body resp)]
      (are [e p] (= e p)
           200 (:code status)
           "test-value" (:test-header headers)
           2 (count body))
      (doseq [s (string headers body)]
        (is (or (= "part1" s) (= "part2" s))))))
  (testing "Backed by queue contract."
    (let [resp (stream-seq *client* :get "http://localhost:8123/stream")
          status (status resp)
          headers (headers resp)]
      (are [e p] (= e p)
           200 (:code status)
           "test-value" (:test-header headers))
      (is (= "part1" (first (string resp))))
      (is (= "part2" (first (string resp)))))))

(deftest issue-1
  (is (= "глава" (string (GET *client* "http://localhost:8123/issue-1")))))

(deftest get-via-proxy
  (let [resp (GET *client* "http://localhost:8123/proxy-req" :proxy {:host "localhost" :port 8123})
        headers (headers resp)]
    (is (= "http://localhost:8123/proxy-req" (:target headers)))))

(deftest proxy-creation
  (testing "host and port missing"
    (is (thrown-with-msg? AssertionError #"Assert failed: host"
          (prepare-request :get "http://not-important/" :proxy {:meaning :less}))))
  (testing "host missing"
    (is (thrown-with-msg? AssertionError #"Assert failed: host"
          (prepare-request :get "http://not-important/" :proxy {:port 8080}))))
  (testing "port missing"
    (is (thrown-with-msg? AssertionError #"Assert failed: port"
          (prepare-request :get "http://not-important/" :proxy {:host "localhost"}))))
  (testing "only host and port"
    (let [r (prepare-request :get "http://not-important/" :proxy {:host "localhost"
                                                                  :port 8080})]
      (is (isa? (class r) com.ning.http.client.Request))))
  (testing "wrong protocol"
    (is (thrown-with-msg? AssertionError #"Assert failed:.*protocol.*"
          (prepare-request :get "http://not-important/" :proxy {:protocol :wrong
                                                                :host "localhost"
                                                                :port 8080}))))
  (testing "http protocol"
    (let [r (prepare-request :get "http://not-important/" :proxy {:protocol :http
                                                                  :host "localhost"
                                                                  :port 8080})]
      (is (isa? (class r) com.ning.http.client.Request))))
  (testing "https protocol"
    (let [r (prepare-request :get "http://not-important/" :proxy {:protocol :https
                                                                  :host "localhost"
                                                                  :port 8383})]
      (is (isa? (class r) com.ning.http.client.Request))))
  (testing "protocol but no host nor port"
    (is (thrown-with-msg? AssertionError #"Assert failed: host"
          (prepare-request :get "http://not-important/" :proxy {:protocol :http}))))
  (testing "host, port, user but no password"
    (is (thrown-with-msg? AssertionError #"Assert failed:.*password.*"
          (prepare-request :get "http://not-important/" :proxy {:host "localhost"
                                                                :port 8080
                                                                :user "name"}))))
  (testing "host, port, password but no user"
    (is (thrown-with-msg? AssertionError #"Assert failed:.*user.*"
          (prepare-request :get "http://not-important/" :proxy {:host "localhost"
                                                                :port 8080
                                                                :password "..."}))))
  (testing "host, port, user and password"
    (let [r (prepare-request :get "http://not-important/" :proxy {:host "localhost"
                                                                  :port 8080
                                                                  :user "name"
                                                                  :password "..."})]
      (is (isa? (class r) com.ning.http.client.Request))))
  (testing "protocol, host, port, user and password"
    (let [r (prepare-request :get "http://not-important/" :proxy {:protocol :http
                                                                  :host "localhost"
                                                                  :port 8080
                                                                  :user "name"
                                                                  :password "..."})]
      (is (isa? (class r) com.ning.http.client.Request)))))

(deftest get-with-cookie
  (let [cv "sample-value"
        resp (GET *client* "http://localhost:8123/cookie"
                  :cookies #{{:domain "http://localhost:8123/"
                              :name "sample-name"
                              :value cv
                              :path "/cookie"
                              :max-age 10
                              :secure false}})
        headers (headers resp)]
    (is (contains? headers :set-cookie))
    (let [cookies (cookies resp)]
      (is (not (empty? cookies)))
      (doseq [cookie cookies]
        (is (or (= (:name cookie) "sample-name")
                (= (:name cookie) "foo")))
        (is (= (:value cookie)
               (if (= (:name cookie) "sample-name")
                 cv
                 "bar")))))))

(deftest get-with-user-agent-branding
  (let [ua-brand "Branded User Agent/1.0"]
    (with-open [client (create-client :user-agent ua-brand)]
      (let [headers (headers (GET client "http://localhost:8123/branding"))]
        (is (contains? headers :x-user-agent))
        (is (= (:x-user-agent headers) ua-brand))))))

(deftest connection-limiting
  (with-open [client (create-client :max-conns-per-host 1
                                      :max-conns-total 1)]
    (let [url "http://localhost:8123/timeout"
          r1 (GET client url)]
      (is (thrown-with-msg? IOException #"Too many connections 1" (GET client url)))
      (is (not (failed? (await r1)))))))

(deftest redirect-convenience-fns
  (let [resp (GET *client* "http://localhost:8123/redirect")]
    (is (true? (redirect? resp)))
    (is (= "http://localhost:8123/here" (location resp)))))

(deftest following-redirect-with-params
  (with-open [client (create-client :remove-params-on-redirect false :follow-redirects true)]
    (let [resp (GET client "http://localhost:8123/redirect" :query {:token "1234"})
          headers (headers resp)]
          (are [x y] (= (x headers) (str y)) :token "1234"))))

(deftest following-redirect-without-params
  (with-open [client (create-client :remove-params-on-redirect true :follow-redirects true)]
    (let [resp (GET client "http://localhost:8123/redirect" :query {:token "1234"})
          headers (headers resp)]
      (is (false? (contains? headers :token))))))

(deftest content-type-fn
  (let [resp (GET *client* "http://localhost:8123/body")]
    (is (.startsWith ^String (content-type resp) "text/plain"))))

(deftest single-set-cookie
  (let [resp (GET *client* "http://localhost:8123/cookie")
        cookie (first (cookies resp))
        header (headers resp)]
    (is (string? (:set-cookie header)))
    (is (= (:name cookie) "foo"))
    (is (= (:value cookie) "bar"))))

(deftest await-string
  (let [resp (GET *client* "http://localhost:8123/stream")
        body (string (await resp))]
    (is (= body "part1part2"))))

(deftest no-host
  (let [resp (GET *client* "http://notexisting/")]
    (await resp)
    (is (= (class (.getCause ^Throwable (error resp))) UnresolvedAddressException))
    (is (true? (failed? resp)))))

(deftest no-realm-for-digest
  (is (thrown-with-msg? IllegalArgumentException #"For DIGEST authentication realm is required"
        (GET *client* "http://not-important/"
             :auth {:type :digest
                    :user "user"
                    :password "secret"}))))

(deftest authentication-without-user-or-password
  (is (thrown-with-msg? IllegalArgumentException #"For authentication user is required"
        (GET *client* "http://not-important/"
             :auth {:password "secret"})))
  (is (thrown-with-msg? IllegalArgumentException #"For authentication password is required"
        (GET *client* "http://not-important/"
             :auth {:user "user"})))
  (is (thrown-with-msg? IllegalArgumentException #"For authentication user and password is required"
        (GET *client* "http://not-important/"
             :auth {:type :basic}))))

(deftest basic-authentication
  (is (=
       (:code (status (GET *client* "http://localhost:8123/basic-auth"
                           :auth {:user "beastie"
                                  :password "boys"})))
       200)))

(deftest preemptive-authentication
  (let [url "http://localhost:8123/preemptive-auth"
        cred {:user "beastie"
              :password "boys"}]
    (testing "Per request configuration"
      (is (=
           (:code (status (GET *client* url
                               :auth cred)))
           401))
      (is (=
           (:code (status (GET *client* url
                               :auth (assoc cred :preemptive true))))
           200)))
    (testing "Global configuration"
      (with-open [c (create-client :auth (assoc cred :preemptive true))]
        (testing "Global preemptive, no per request"
          (is (= 200
                 (:code (status (GET c url))))))
        (testing "Global preeptive, per request preemptive"
          (is (= 200
                 (:code (status (GET c url :auth (assoc cred :preemptive true)))))))
        (testing "Global preemptive, per request no preemptive"
          (is (= 401
                 (:code (status (GET c url :auth (assoc cred :preemptive false)))))))))))

(deftest canceling-request
  (let [resp (GET *client* "http://localhost:8123/")]
    (is (false? (cancelled? resp)))
    (is (true? (cancel resp)))
    (await resp)
    (is (true? (cancelled? resp)))
    (is (true? (done? resp)))))

(deftest reqeust-timeout
  (testing "timing out"
    (let [resp (GET *client* "http://localhost:8123/timeout" :timeout 100)]
      (await resp)
      (is (true? (failed? resp)))
      (if (failed? resp)
        (is (instance? TimeoutException (error resp)))
        (println "headers of response that was supposed to timeout." (headers resp)))))
  (testing "infinite timeout"
    (let [resp (GET *client* "http://localhost:8123/timeout" :timeout -1)]
      (await resp)
      (is (not (failed? resp)))
      (if (failed? resp)
        (do
          (println "Delivered error:" (realized? (:error resp)))
          (print-stack-trace (error resp))))
      (is (true? (done? resp)))))
  (testing "global timeout"
    (with-open [client (create-client :request-timeout 100)]
      (let [resp (GET client "http://localhost:8123/timeout")]
        (await resp)
        (is (true? (failed? resp)))
        (if (failed? resp)
          (is (instance? TimeoutException (error resp)))
          (println "headers of response that was supposed to timeout" (headers resp))))))
  (testing "global timeout overwritten by local infinite"
    (with-open [client (create-client :request-timeout 100)]
      (let [resp (GET client "http://localhost:8123/timeout" :timeout -1)]
        (await resp)
        (is (false? (failed? resp)))
        (is (done? resp)))))
  (testing "global idle connection in pool timeout"
    (with-open [client (create-client :idle-in-pool-timeout 100)]
      (let [resp (GET client "http://localhost:8123/timeout")]
        (await resp)
        (is (false? (failed? resp)))
        (when (failed? resp)
          (println "No response received, while excepting it." (.getMessage ^Throwable (error resp))))))))

(deftest connection-timeout
  ;; timeout connection after 1ms
  (with-open [client (create-client :connection-timeout 1)]
    (let [resp (GET client "http://localhost:8124/")]
      (await resp)
      (is (true? (failed? resp)))
      (is (instance? ConnectException (error resp))))))

(deftest closing-client
  (let [client (create-client)
        _ (await (GET client "http://localhost:8123/"))]
    (close client)
    (is (thrown-with-msg? IOException #"Closed" (GET client "http://localhost:8123/")))))

(deftest extract-empty-body
  (let [resp (GET *client* "http://localhost:8123/empty")]
    (is (nil? (string resp)))))

(deftest response-url
  (let [resp (GET *client* "http://localhost:8123/query" :query {:a "1?&" :b "+ ="})]
    (are [exp real] (= exp real)
         "http://localhost:8123/query?a=1?&&b=+ =" (raw-url resp)
         "http://localhost:8123/query?a=1%3F%26&b=%2B%20%3D" (url resp))))

;;(deftest profile-get-stream
;;  (let [gets (repeat (GET *client* "http://localhost:8123/stream"))
;;        seqs (repeat (stream-seq *client* :get "http://localhost:8123/stream"))
;;        f (fn [resps] (doseq [resp resps] (is (= "part1part2" (prof :get-stream (string resp))))))
;;        g (fn [resps] (doseq [resp resps] (doseq [s (prof :seq-stream (doall (string resp)))]
;;                                                (is (or (= "part1" s) (= "part2 s"))))))]
;;    (profile (dotimes [i 10]
;;               (f (take 1000 (nthnext gets (* i 1000))))
;;               (g (take 1000 (nthnext seqs (* i 1000))))))))
