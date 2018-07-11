(ns consul.proto.event
  "Interface to com.orbitz.consul.EventClient.")

(defprotocol IEvent
  (fire-event! [this name] [this name options] [this name options payload])
  (list-events [this] [this name-or-opts] [this name opts]))