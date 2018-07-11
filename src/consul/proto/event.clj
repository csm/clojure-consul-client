(ns consul.proto.event
  "Interface to com.orbitz.consul.EventClient.")

(defprotocol IEvent
  (fire-event! [this name] [this name options] [this name options payload]
    "Fire an event.

    name is a string name of the event.

    options can either be an instance of com.orbitz.consul.option.EventOptions,
    or a map spec containing keys:

    * :datacenter     The datacenter name.
    * :node-filter    The node filter string.
    * :service-filter The service filter string.
    * :tag-filter     The tag filter string.

    payload may be a string giving the event payload.")
  (list-events [this] [this name-or-opts] [this name opts]
    "List events.

    The name-or-opts argument can either be an event name, or a QueryOptions
    instance, or a map spec for consul.core/query-options.

    If both name and opts are given, the name should be an event name, and
    opts a QueryOptions instance or map spec."))