# clojure-consul-client

[![Clojars Project](https://img.shields.io/clojars/v/com.github.csm/clojure-consul-client.svg)](https://clojars.org/com.github.csm/clojure-consul-client) [![cljdoc](https://cljdoc.org/badge/com.github.csm/clojure-consul-client)](https://cljdoc.org/d/com.github.csm/clojure-consul-client)

A synchronous and asynchronous [Consul](https://consul.io) client
built on [vainglory](https://github.com/csm/vainglory) and
[consul-api](https://github.com/csm/consul-api).

```clojure
(require '[consul.client :as consul])

(def client (consul/consul {}))

; Get a map describing supported operations
(consul/ops client)
(:listDatacenters (consul/ops client))

; Invoke an operation.
(consul/invoke {:op :listDatacenters})
(consul/invoke {:op :writeKey :request {:key "test" :body "something for this key"}})
(consul/invoke {:op :readKey :request {:key "test"}})

; Or go async
(require '[consul.client.async :as ca])
(require '[manifold.deferred :as d])

(d/chain
  (ca/invoke client {:op :readKey :request {:key "test" :index 1234 :wait "15s"}})
  (fn [response] ...))
```
