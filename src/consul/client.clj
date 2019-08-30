(ns consul.client
  "Synchronous consul client."
  (:require [clojure.java.io :as io]
            [vainglory.core :as v]))

(def ^:private api-spec (delay (v/load-json (io/resource "consul-api/swagger.json"))))

(defn consul
  "Create a consul client, suitable for synchronous usage
  with [[invoke]], or asynchronous usage with [[consul.client.async/invoke]].

  Use [[ops]] to fetch a list of supported operations, and the arguments
  they take.

  Argument map may contain the following keys:

  * `:scheme` -- `:http` or `:https`. Defaults to `:http`.
  * `:host` -- host to connect to. Defaults to localhost.
  * `:port` -- port to connect to. Defaults to 8500.
  * `:conn-pool` -- an `aleph.http/connection-pool`."
  [{:keys [scheme host port] :or {scheme :http
                                  host "localhost"
                                  port 8500}
    :as arg-map}]
  {:client (v/client @api-spec (assoc arg-map :api-id "consul"))
   :scheme scheme
   :host host
   :port 8500})

(defn invoke
  "Invoke a consul API call.

  Arguments in arg-map include:

  * `:op` A keyword naming the operation to invoke. Required.
  * `:request` A map containing the request to pass; required based on operation -- see [[ops]] for
     a way to discover what arguments are required.
  * `:headers` A map of strings to strings, HTTP headers to include
    in the request.
  * `:decode-key-fn` -- function to use to decode JSON keys; default leaves keys as is; can be
    `true` to keywordize keys, or any arbitrary 1-arg function.

  Returns a map of the response, possibly with a `:body`, on success;
  returns an anomaly map on failure."
  [client arg-map]
  (v/invoke (:client client) (merge arg-map (dissoc client :client))))

(defn ops
  "Return a map of operations this client supports."
  [client]
  (v/ops (:client client)))