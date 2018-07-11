(ns consul.proto.session
  "Interface to com.orbitz.consul.SessionClient")

(defprotocol ISession
  (create-session [this session] [this session dc])
  (renew-session [this id] [this id dc])
  (destroy-session [this id] [this id dc])
  (session-info [this id] [this id dc])
  (list-sessions [this] [this dc]))