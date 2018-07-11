(ns consul.proto.catalog
  "Interface to com.orbitz.consul.CatalogClient.

  Many API calls in ICatalog return a response map, which contains
  keys :index, :knownLeader, :lastContact, and :response. The
  :response key maps the return value of each call.

  API calls that take query option arguments may take either a
  com.orbitz.consul.options.QueryOptions instance, or a map
  spec containing keys:

  * :wait
  * :token
  * :index
  * :near
  * :datacenter
  * :node-meta
  * :tags
  * :consistency-mode")

(defprotocol ICatalog
  (datacenters [this]
    "Return a list of datacenter strings.")
  (nodes [this] [this opts]
    "Return a list of nodes connected to this agent.

    The response the list of nodes, each a map giving the node info.

    The opts argument may be a com.orbitz.consul.options.QueryOptions
    instance, or a map spec of the same.")
  (services [this] [this opts]
    "Return a list of services known to this agent.

    The response is a map of service names to lists of tags.

    The opts argument may be a com.orbitz.consul.options.QueryOptions
    instance, or a map spec of the same.")
  (service [this service-name] [this service-name opts]
    "Return info about a service.

    The result is a list of service catalog entries.

    The opts argument may be a com.orbitz.consul.options.QueryOptions
    instance, or a map spec of the same.")
  (node [this node-name] [this node-name opts]
    "Return info about a node.

    The result is a map containing info about the node, and services
    known about this node.")
  (register! [this args] [this args opts]
    "Register a node with the catalog.

    The args argument can either be an instance of
    com.orbitz.consul.model.catalog.CatalogRegistration, or a map
    spec containing keys:

    * :datacenter
    * :node
    * :address
    * :tagged-addresses
    * :service
    * :check
    * :write-request")
  (deregister! [this args] [this args opts]
    "Deregister a node from the catalog.

    The args argument can either be an instance of
    com.orbitz.consul.model.catalog.CatalogDeregistration, or a map
    spec containing keys:

    * :datacenter
    * :node
    * :check-id
    * :service-id
    * :write-request"))