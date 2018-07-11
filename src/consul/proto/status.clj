(ns consul.proto.status
  "Interface to com.orbitz.consul.StatusClient")

(defprotocol IStatus
  (leader [this] "Return the host/port of the leader.")
  (peers [this] "Return a list of host/port values of peers."))