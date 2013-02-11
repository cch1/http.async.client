http.async.client - Asynchronous HTTP Client for Clojure
========================================================

Master: [![Master - Build Status](https://secure.travis-ci.org/neotyk/http.async.client.png?branch=master)](http://travis-ci.org/neotyk/http.async.client)
Development: [![Development - Build Status](https://secure.travis-ci.org/neotyk/http.async.client.png?branch=development)](http://travis-ci.org/neotyk/http.async.client)

## *tl;dr*
Declare dependency:

``` clojure
(defproject your-project "1.0.0-SNAPSHOT"
  :description "Your project description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [http.async.client "0.5.2"]])
```

Require:

``` clojure
(ns sample (:require [http.async.client :as http]))
```

GET resource:

``` clojure
(with-open [client (http/create-client)]
  (let [response (http/GET client "http://neotyk.github.com/http.async.client/")]
    (-> response
      http/await
      http/string)))
```

## Information over *http.async.client*

[*http.async.client*](http://github.com/neotyk/http.async.client) is
based on [Asynchronous Http Client for Java](http://github.com/sonatype/async-http-client).

It runs with Clojure 1.3, development goes against Clojure 1.4.

For more documentation refer to
 [docs](http://neotyk.github.com/http.async.client/docs.html) and for
 API to [doc](http://neotyk.github.com/http.async.client/doc/).

[Discussion forum](http://groups.google.com/group/httpasyncclient).

If you would like to help please
[look at to do](http://neotyk.github.com/http.async.client/todo.html)
or
[submit a ticket](http://github.com/neotyk/http.async.client/issues).

[*http.async.client*](http://github.com/neotyk/http.async.client) is
using [*YourKit*](http://www.yourkit.com/).

*http.async.client* is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

[Changelog](http://neotyk.github.com/http.async.client/changelog.html).

[Contributors](https://github.com/neotyk/http.async.client/graphs/contributors).
