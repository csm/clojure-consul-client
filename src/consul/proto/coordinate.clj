(ns consul.proto.coordinate
  "Interface to com.orbitz.consul.CoordinateClient")

(defprotocol ICoordinate
  (datacenters [this]
    "Return a sequence of datacenters.")
  (nodes [this] [this dc]
    "Return a sequence of nodes.

    If no datacenter argument is given, the datacenter of the agent connected
    to is used."))