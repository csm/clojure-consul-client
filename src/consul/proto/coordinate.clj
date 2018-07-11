(ns consul.proto.coordinate
  "Interface to com.orbitz.consul.CoordinateClient")

(defprotocol ICoordinate
  (datacenters [this])
  (nodes [this] [this dc]))