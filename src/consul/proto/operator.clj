(ns consul.proto.operator
  "Interface to com.orbitz.consul.OperatorClient.")

(defprotocol IOperator
  (raft-config [this] [this dc])
  (stale-raft-config [this] [this dc])
  (delete-peer! [this address] [this address dc]))