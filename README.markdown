http.async.client - Asynchronous HTTP Client for Clojure
========================================================

## *tl;dr*
Declare dependency:

    (defproject your-project "1.0.0-SNAPSHOT"
      :description "Your project description"
      :dependencies [[org.clojure/clojure "1.2.0"]
                     [org.clojure/clojure-contrib "1.2.0"]
                     [http.async.client "0.2.2"]])

Require:

    (ns sample (:require [http.async.client :as c]))

GET resource:

    (let [response (c/GET "http://github.com/neotyk/http.async.client/")]
      (c/await response)
      (c/string response))

## Information over *http.async.client*

[*http.async.client*](http://github.com/neotyk/http.async.client) is
based on [Asynchronous Http Client for Java](http://github.com/AsyncHttpClient/async-http-client)
which runs on top of [Netty Project](http://jboss.org/netty).

It requires Clojure 1.2.

For more documentation refer to
 [docs](http://neotyk.github.com/http.async.client/docs.html) and for
 API to [autodoc](http://neotyk.github.com/http.async.client/autodoc/).


*http.async.client* is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

If you would like to help please look at
[to do](http://neotyk.github.com/http.async.client/todo.html) or submit
ticket [here](http://github.com/neotyk/http.async.client/issues).

[Changelog](http://neotyk.github.com/http.async.client/changelog.html).
