http.async.client - Asynchronous HTTP Client for Clojure
========================================================

Master: [![Master - Build Status](https://secure.travis-ci.org/neotyk/http.async.client.png?branch=master)](http://travis-ci.org/neotyk/http.async.client)
Development: [![Development - Build Status](https://secure.travis-ci.org/neotyk/http.async.client.png?branch=development)](http://travis-ci.org/neotyk/http.async.client)

## *tl;dr*
Declare dependency:

``` clojure
(defproject your-project "1.0.0-SNAPSHOT"
  :description "Your project description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [http.async.client "0.4.1"]])
```

Require:

``` clojure
(ns sample (:require [http.async.client :as c]))
```

GET resource:

``` clojure
(with-open [client (c/create-client)]
  (let [response (c/GET client "http://neotyk.github.com/http.async.client/")]
    (c/await response)
    (c/string response)))
```

## Information over *http.async.client*

[*http.async.client*](http://github.com/neotyk/http.async.client) is
based on [Asynchronous Http Client for Java](http://github.com/AsyncHttpClient/async-http-client).

It requires Clojure 1.3, works with 1.4-beta1.

For more documentation refer to
 [docs](http://neotyk.github.com/http.async.client/docs.html) and for
 API to [doc](http://neotyk.github.com/http.async.client/doc/).


*http.async.client* is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

If you would like to help please look at
[to do](http://neotyk.github.com/http.async.client/todo.html) or submit
ticket [here](http://github.com/neotyk/http.async.client/issues).

[Changelog](http://neotyk.github.com/http.async.client/changelog.html).
