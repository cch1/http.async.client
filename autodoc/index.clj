{:namespaces
 ({:source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc/http.async.client-api.html",
   :name "http.async.client",
   :author "Hubert Iwaniuk",
   :doc "Asynchronous HTTP Client - Clojure"}
  {:source-url
   "http://github.com/neotyk/http.async.client/blob/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/headers.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc/http.async.client.headers-api.html",
   :name "http.async.client.headers",
   :author "Hubert Iwaniuk",
   :doc "Asynchrounous HTTP Client - Clojure - Lazy headers"}
  {:source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc/http.async.client.request-api.html",
   :name "http.async.client.request",
   :author "Hubert Iwaniuk",
   :doc "Asynchronous HTTP Client - Clojure - Requesting API"}
  {:source-url
   "http://github.com/neotyk/http.async.client/blob/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/status.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc/http.async.client.status-api.html",
   :name "http.async.client.status",
   :author "Hubert Iwaniuk",
   :doc "Asynchronous HTTP Client - Clojure - Lazy status."}
  {:source-url
   "http://github.com/neotyk/http.async.client/blob/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc/http.async.client.util-api.html",
   :name "http.async.client.util",
   :author "Hubert Iwaniuk",
   :doc "Asynchronous HTTP Client - Clojure - Utils"}),
 :vars
 ({:arglists ([client url & {:as options}]),
   :name "DELETE",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/DELETE",
   :doc
   "Sends asynchronously HTTP DELETE request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client url & {:as options}]),
   :name "GET",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/GET",
   :doc
   "Sends asynchronously HTTP GET request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client url & {:as options}]),
   :name "HEAD",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/HEAD",
   :doc
   "Sends asynchronously HTTP HEAD request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client url & {:as options}]),
   :name "OPTIONS",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/OPTIONS",
   :doc
   "Sends asynchronously HTTP OPTIONS request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client url & {:as options}]),
   :name "POST",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/POST",
   :doc
   "Sends asynchronously HTTP POST request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client url & {:as options}]),
   :name "PUT",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L145",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/PUT",
   :doc
   "Sends asynchronously HTTP PUT request to url.\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.\nArguments:\n- client   - client created via create-client\n- url      - URL to request\n- options  - keyworded arguments:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type     - either :basic or :digest\n    :user     - user name to be used\n    :password - password to be used\n    :realm    - realm name to authenticate in\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 145,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([response]),
   :name "await",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L196",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/await",
   :doc
   "Waits for response processing to be finished.\nReturns same response.",
   :var-type "function",
   :line 196,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "body",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L209",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/body",
   :doc
   "Gets body.\nIf body have not yet been delivered and request hasn't failed waits for body.",
   :var-type "function",
   :line 209,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "cancel",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L261",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/cancel",
   :doc "Cancels response.",
   :var-type "function",
   :line 261,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "cancelled?",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L255",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/cancelled?",
   :doc "Checks if response has been cancelled.",
   :var-type "function",
   :line 255,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client]),
   :name "close",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L267",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/close",
   :doc "Closes client.",
   :var-type "function",
   :line 267,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "cookies",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L239",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/cookies",
   :doc "Gets cookies from response.",
   :var-type "function",
   :line 239,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists
   ([&
     {:keys
      [compression-enabled
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
       executor-service]}]),
   :name "create-client",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L26",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/create-client",
   :doc
   "Creates new Async Http Client.\nArguments:\n- :compression-enabled :: enable HTTP compression\n- :connection-timeout :: connections timeout in ms\n- :follow-redirects :: enable following HTTP redirects\n- :idle-in-pool-timeout :: idle connection in pool timeout in ms\n- :keep-alive :: enable HTTP keep alive, enabled by default\n- :max-conns-per-host :: max number of polled connections per host\n- :max-conns-total :: max number of total connections held open by client\n- :max-redirects :: max nuber of redirects to follow\n- :proxy :: map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n- :auth :: map with authentication to be used\n    :type       - either :basic or :digest\n    :user       - user name to be used\n    :password   - password to be used\n    :realm      - realm name to authenticate in\n    :preemptive - assume authentication is required\n- :request-timeout :: request timeout in ms\n- :user-agent :: User-Agent branding string\n- :async-connect :: Execute connect asynchronously\n- :executor-service :: provide your own executor service for callbacks to be executed on",
   :var-type "function",
   :line 26,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "done?",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L180",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/done?",
   :doc
   "Checks if request is finished already (response receiving finished).",
   :var-type "function",
   :line 180,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "error",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L249",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/error",
   :doc "Returns Throwable if request processing failed.",
   :var-type "function",
   :line 249,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "failed?",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L175",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/failed?",
   :doc "Checks if request failed.",
   :var-type "function",
   :line 175,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "headers",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L203",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/headers",
   :doc
   "Gets headers.\nIf headers have not yet been delivered and request hasn't failed waits for headers.",
   :var-type "function",
   :line 203,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client method url body-part-callback & {:as options}]),
   :name "request-stream",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L147",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/request-stream",
   :doc
   "Consumes stream from given url.\nmethod - HTTP method to be used (:get, :post, ...)\nurl - URL to set request to\nbody-part-callback - callback that takes status (ref {}) of request\n                     and received body part as vector of bytes\noptions - are optional and can contain :headers, :param, and :query (see prepare-request).",
   :var-type "function",
   :line 147,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp]),
   :name "status",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L244",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/status",
   :doc "Gets status if status was delivered.",
   :var-type "function",
   :line 244,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([client method url & {:as options}]),
   :name "stream-seq",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L159",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/stream-seq",
   :doc "Creates potentially infinite lazy sequence of Http Stream.",
   :var-type "function",
   :line 159,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([resp] [headers body]),
   :name "string",
   :namespace "http.async.client",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj#L230",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/796c504ba6c73c3e6b2cbe9aae601f8af07d888a/src/clj/http/async/client.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client/string",
   :doc
   "Converts response to string.\nOr converts body taking encoding from response.",
   :var-type "function",
   :line 230,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client.clj"}
  {:arglists ([headers]),
   :name "convert-headers-to-map",
   :namespace "http.async.client.headers",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/headers.clj#L29",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/headers.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.headers/convert-headers-to-map",
   :doc "Converts Http Response Headers to lazy map.",
   :var-type "function",
   :line 29,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/headers.clj"}
  {:arglists ([headers]),
   :name "create-cookies",
   :namespace "http.async.client.headers",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/headers.clj#L60",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/headers.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.headers/create-cookies",
   :doc "Creates cookies from headers.",
   :var-type "function",
   :line 60,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/headers.clj"}
  {:raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L87",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/*default-callbacks*",
   :namespace "http.async.client.request",
   :line 87,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj",
   :var-type "var",
   :doc "Default set of callbacks.",
   :name "*default-callbacks*"}
  {:arglists ([action]),
   :name "convert-action",
   :namespace "http.async.client.request",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L182",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/convert-action",
   :doc "Converts action (:abort, nil) to Async client STATE.",
   :var-type "function",
   :line 182,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj"}
  {:arglists
   ([client
     req
     &
     {status :status,
      headers :headers,
      part :part,
      completed :completed,
      error :error}]),
   :name "execute-request",
   :namespace "http.async.client.request",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L190",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/execute-request",
   :doc
   "Executes provided request.\nArguments:\n- req        - request to be executed\n- :status    - status callback\n- :headers   - headers callback\n- :part      - body part callback\n- :completed - response completed\n- :error     - error callback\n\nReturns a map:\n- :id      - unique ID of request\n- :status  - promise that once status is received is delivered, contains lazy map of:\n  - :code     - response code\n  - :msg      - response message\n  - :protocol - protocol with version\n  - :major    - major version of protocol\n  - :minor    - minor version of protocol\n- :headers - promise that once headers are received is delivered, contains lazy map of:\n  - :server - header names are keyworded, values stay not changed\n- :body    - body of response, depends on request type, might be ByteArrayOutputStream\n             or lazy sequence, use conveniece methods to extract it, like string\n- :done    - promise that is delivered once receiving response has finished\n- :error   - promise that is delivered if requesting resource failed, once delivered\n             will contain Throwable.",
   :var-type "function",
   :line 190,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj"}
  {:arglists ([{ct :content-type, :or {ct ""}}]),
   :name "get-encoding",
   :namespace "http.async.client.request",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L51",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/get-encoding",
   :doc
   "Gets content encoding from headers, if Content-Type header not present\nor media-type in it is missing => nil",
   :var-type "function",
   :line 51,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj"}
  {:arglists
   ([method
     url
     &
     {:keys [headers query body cookies proxy auth timeout]}]),
   :name "prepare-request",
   :namespace "http.async.client.request",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L103",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/prepare-request",
   :doc
   "Prepares method (GET, POST, ..) request to url.\nOptions:\n  :query   - map of query parameters\n  :headers - map of headers\n  :body    - body\n  :cookies - cookies to send\n  :proxy   - map with proxy configuration to be used\n    :host     - proxy host\n    :port     - proxy port\n    :protocol - (optional) protocol to communicate with proxy,\n                :http (default, if you provide no value) and :https are allowed\n    :user     - (optional) user name to use for proxy authentication,\n                has to be provided with :password\n    :password - (optional) password to use for proxy authentication,\n                has to be provided with :user\n  :auth    - map with authentication to be used\n    :type       - either :basic or :digest\n    :user       - user name to be used\n    :password   - password to be used\n    :realm      - realm name to authenticate in\n    :preemptive - assume authentication is required\n  :timeout - request timeout in ms",
   :var-type "function",
   :line 103,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj"}
  {:arglists ([arg]),
   :name "url-encode",
   :namespace "http.async.client.request",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj#L96",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/820f235a940a94185a24f97f074aff4df50a6022/src/clj/http/async/client/request.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.request/url-encode",
   :doc "Taken from Clojure Http Client",
   :var-type "function",
   :line 96,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/request.clj"}
  {:arglists ([st]),
   :name "convert-status-to-map",
   :namespace "http.async.client.status",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/status.clj#L20",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/3eeb9426f7368c59579a0bdb1a41f3101d361096/src/clj/http/async/client/status.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.status/convert-status-to-map",
   :doc "Convert HTTP Status line to lazy map.",
   :var-type "function",
   :line 20,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/status.clj"}
  {:arglists ([p]),
   :name "delivered?",
   :namespace "http.async.client.util",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj#L50",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.util/delivered?",
   :doc
   "Alpha - subject to change.\nReturns true if promise has been delivered, else false",
   :var-type "function",
   :line 50,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/util.clj"}
  {:arglists ([]),
   :name "promise",
   :namespace "http.async.client.util",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj#L24",
   :added "1.1",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.util/promise",
   :doc
   "Alpha - subject to change.\nReturns a promise object that can be read with deref/@, and set,\nonce only, with deliver. Calls to deref/@ prior to delivery will\nblock. All subsequent derefs will return the same delivered value\nwithout blocking.",
   :var-type "function",
   :line 24,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/util.clj"}
  {:arglists ([{:keys [protocol host port user password]} b]),
   :name "set-proxy",
   :namespace "http.async.client.util",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj#L63",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.util/set-proxy",
   :doc "Sets proxy on builder.",
   :var-type "function",
   :line 63,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/util.clj"}
  {:arglists
   ([{:keys [type user password realm preemptive], :or {:type :basic}}
     b]),
   :name "set-realm",
   :namespace "http.async.client.util",
   :source-url
   "http://github.com/neotyk/http.async.client/blob/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj#L80",
   :raw-source-url
   "http://github.com/neotyk/http.async.client/raw/f3a96d0d265a610771172601f7633ff1e6c6999c/src/clj/http/async/client/util.clj",
   :wiki-url
   "http://neotyk.github.com/http.async.client/autodoc//http.async.client-api.html#http.async.client.util/set-realm",
   :doc "Sets realm on builder.",
   :var-type "function",
   :line 80,
   :file
   "/Users/neotyk/Source/clojure/http.async.client/src/clj/http/async/client/util.clj"})}
