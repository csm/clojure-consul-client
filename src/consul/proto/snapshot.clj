(ns consul.proto.snapshot
  "Interface to com.orbitz.consul.SnapshotClient")

(defprotocol ISnapshot
  (save! [this dest query-options])
  (restore! [this source query-options]))