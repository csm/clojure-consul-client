# clojure-consul-client

[![Clojars Project](https://img.shields.io/clojars/v/com.github.csm/clojure-consul-client.svg)](https://clojars.org/com.github.csm/clojure-consul-client)

A clojure wrapper around [this Java consul client](https://github.com/rickfast/consul-client).

## Usage

The primary purpose is to adapt the clients made available in the Java client to Clojure protocols, and to have the
Clojure versions deal in simple data types (maps, lists, etc.) instead of the typed model classes of the Java library.

```clojure
; connect to consul. You can pass various options for connecting here.
(require '[consul.core :as consul])
(def c (consul/consul))

; Get a health client.
(require '[consul.proto :refer :all])
(def h (health-client c))

; The above returns the java object, and you can use it as such
(.getServiceChecks h "my-service")

; Or you can use the protocol
(require '[consul.proto.health :as health])
(health/service-checks h {:wait "1m" :index my-index})
```

## License

Copyright © 2018 Noon Home, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
