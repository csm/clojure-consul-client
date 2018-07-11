(ns consul.proto.catalog)

(defprotocol ICatalog
  (datacenters [this])
  (nodes [this] [this opts])
  (services [this] [this opts])
  (service [this service-name] [this service-name opts])
  (node [this node-name] [this node-name opts])
  (register! [this args] [this args opts])
  (deregister! [this args] [this args opts]))