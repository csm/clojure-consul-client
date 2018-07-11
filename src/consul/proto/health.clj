(ns consul.proto.health)

(defprotocol IHealth
  (node-checks [this node] [this node opts])
  (service-checks [this service] [this service opts])
  (checks-by-state [this state] [this state opts])
  (healthy-service-instances [this service] [this service opts])
  (service-instances [this service] [this service opts]))