(ns consul.proto.prepared-query
  "Interface to com.orbitz.consul.PreparedQueryClient")

(defprotocol IPreparedQuery
  (prepare-query! [this query] [this query dc])
  (prepared-queries [this] [this dc])
  (prepared-query [this id] [this id dc])
  (execute! [this name-or-id]))