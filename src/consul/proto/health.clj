(ns consul.proto.health
  "Interface to com.orbitz.consul.HealthClient.")

(defprotocol IHealth
  (node-checks [this node] [this node opts]
    "Fetch checks for a node by name.

    If opts is given, it can be a QueryOptions instance, or a map spec.")
  (service-checks [this service] [this service opts]
    "Fetch checks for a service by name.

    If opts is given, it can be a QueryOptions instance, or a map spec.")
  (checks-by-state [this state] [this state opts]
    "Fetch checks by state.

    state may be a com.orbitz.consul.model.State instance, or a string representing
    the state.

    opts may be a QueryOptions instance, or a map spec.")
  (healthy-service-instances [this service] [this service opts]
    "Fetch all instances for a service that are currently healthy.

    If opts is given, it can be a QueryOptions instance, or a map spec.")
  (service-instances [this service] [this service opts]
    "Fetch all instances for a service.

    If opts is given, it can be a QueryOptions instance, or a map spec."))