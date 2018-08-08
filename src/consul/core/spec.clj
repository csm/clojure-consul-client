(ns consul.core.spec
  (:require [clojure.spec.alpha :as s])
  (:import [com.google.common.net HostAndPort]))

(s/def ::host-and-port #(instance? HostAndPort %))

(s/def ::host (s/and string?))
(s/def ::port (s/int-in 0 65536))

(s/fdef consul.core/host-and-port
  :args (s/alt :instance ::host-and-port
               :string string?
               :parts vector?
               :map (s/keys :req-un [::host] :opt-un [::port]))
  :ret ::host-and-port)
