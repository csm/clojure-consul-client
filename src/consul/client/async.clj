(ns consul.client.async
  "Asynchronous consul client."
  (:require [vainglory.async :as v]))

(defn invoke
  "Invoke an asynchronous Consul API call.

  Arguments and return values are equivalent to those
  for [[consul.client/invoke]], except this function
  returns a manifold deferred that will yield the
  response."
  [client arg-map]
  (v/invoke (:client client) (merge arg-map (dissoc arg-map :client))))