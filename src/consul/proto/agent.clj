(ns consul.proto.agent
  "Interface to com.orbitz.consul.AgentClient.")

(defprotocol IAgent
  (registered? [this service] "Tell if the service with the given name is registered. Returns a boolean.")
  (ping [this] "Ping the agent.")
  (register! [this registration] [this registration query-opts]
    "Register a service.

    The registration argument may be a com.orbitz.consul.model.agent.Registration,
    or a map containing keys:

    * :name      The service name.
    * :id        A service identifier.
    * :address   The address of the service, if different from the agent address.
    * :port      The port of the service.
    * :checks    A sequence of com.orbitz.consul.model.agent.Registration$RegCheck
                 instances, or of maps that spec a RegCheck (see below).
    * :tags      A sequence of tag strings.
    * :meta      A metadata map of string->string.
    * :enable-tag-override A boolean.

    A check spec is a map that contains the following keys:

    * :script    The script to run for the check.
    * :interval  A string giving the check interval.
    * :ttl       A string giving the time-to-live interval.
    * :tcp       A string giving the TCP check of the service.
    * :http      A string giving the HTTP check of the service.
    * :grpc      A string giving the gRPC check of the service.
    * :grpc-use-tls? A boolean.
    * :timeout   A string giving the check timeout.
    * :notes     Notes about the check.
    * :deregister-critical-services-after A string.
    * :tls-skip-verify A boolean.
    * :status    A string.

    If query-opts is given, it can be a com.orbitz.consul.option.QueryOption
    instance, or a map spec of the same.")
  (deregister! [this service-id] [this service-id query-opts]
    "Deregister a service.

    The service-id argument is the ID of the service to deregister.

    If query-opts is given, it can be a com.orbitz.consul.option.QueryOption
    instance, of a map spec of the same.")
  (register-check! [this args]
    "Register a check.

    The argument args can either be a com.orbitz.consul.model.agent.Check,
    or a map spec with the following keys:

    * :id         A string check ID.
    * :name       A check name.
    * :notes      A string.
    * :output     A string.
    * :script     The check script, a string.
    * :interval   The check interval string.
    * :ttl        The check TTL string.
    * :http       The HTTP check string.
    * :tcp        The TCP check string.
    * :grpc       The gRPC check string.
    * :grpc-use-tls? A boolean.
    * :service-id The service ID.
    * :service-tags A sequence of service tag strings.
    * :deregister-critical-services-after")
  (deregister-check! [this check-id]
    "Deregister a check. The argument is the ID string of the check.")
  (agent [this]
    "Return a map of info about the agent.")
  (checks [this]
    "Return a map of checks on this agent.")
  (services [this]
    "Return a map of all services on this agent.")
  (members [this]
    "Return a sequence of all members this agent is connected to.")
  (force-leave! [this node-id]
    "Force node with ID node-id to leave.")
  (check! [this check-id state note]
    "Set the status of a check.

    check-id is the ID of the check, state is either an instance of
    com.orbitz.consul.model.State, or a string representing one of
    the states; note is an optional note string.")
  (check-ttl! [this service-id state note]
    "Set the status of a TTL check for a service.

    service-id is the ID of the service to check in for; state is
    either an instance of com.orbitz.consul.model.State, or a string
    representing one of the states; note is an optional note string.")
  (pass! [this service-id] [this service-id note]
    "Set a TTL check for a service, with state State/PASS.")
  (warn! [this service-id] [this service-id note]
    "Set a TTL check for a service, with state State/WARN.")
  (fail! [this service-id] [this service-id note]
    "Set a TTL check for a service, with state State/FAIL.")
  (pass-check! [this check-id] [this check-id note]
    "Set a check to state State/PASS.")
  (warn-check! [this check-id] [this check-id note]
    "Set a check to state State/WARN.")
  (fail-check! [this check-id] [this check-id note]
    "Set a check to state State/FAIL.")
  (join! [this address] [this address wan?]
    "Join another agent, given by address.

    wan? may be set to true to join across datacenters.")
  (maintenance-mode! [this service-id enable?] [this service-id enable? reason]
    "Enable maintenance mode."))