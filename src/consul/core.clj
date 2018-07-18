(ns consul.core
  "Core consul constructor functions."
  (:require [consul.impl :refer :all]
            [consul.proto :refer [IConsul]]
            [consul.proto.acl :as acl]
            [consul.proto.agent :as agent]
            [consul.proto.catalog :as catalog]
            [consul.proto.coordinate :as coordinate]
            [consul.proto.event :as event]
            [consul.proto.health :as health]
            [consul.proto.kv :as kv]
            [consul.proto.operator :as operator]
            [consul.proto.prepared-query :as pq]
            [consul.proto.session :as session]
            [consul.proto.snapshot :as snapshot]
            [consul.proto.status :as status]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [com.orbitz.consul Consul Consul$Builder AgentClient CatalogClient AclClient HealthClient KeyValueClient StatusClient SessionClient EventClient PreparedQueryClient CoordinateClient OperatorClient SnapshotClient]
           [com.google.common.net HostAndPort]
           [com.orbitz.consul.config ClientConfig CacheConfig CacheConfig$Builder]
           [com.orbitz.consul.model.agent Registration ImmutableRegistration ImmutableRegistration$Builder Registration$RegCheck ImmutableRegCheck ImmutableRegCheck$Builder Check ImmutableCheck]
           [com.orbitz.consul.option QueryOptions ImmutableQueryOptions ImmutableQueryOptions$Builder ConsistencyMode TransactionOptions ImmutableTransactionOptions EventOptions ImmutableEventOptions]
           [com.orbitz.consul.model.catalog CatalogRegistration ImmutableCatalogRegistration TaggedAddresses ImmutableTaggedAddresses WriteRequest ImmutableWriteRequest CatalogDeregistration ImmutableCatalogDeregistration]
           [com.orbitz.consul.model.acl AclToken ImmutableAclToken]
           [com.orbitz.consul.model State]
           [java.nio.charset Charset]
           [com.orbitz.consul.model.kv Operation ImmutableOperation]
           [com.orbitz.consul.model.session Session ImmutableSession]
           [com.orbitz.consul.model.query PreparedQuery ImmutablePreparedQuery Template ImmutableTemplate DnsQuery ImmutableDnsQuery]
           [com.orbitz.consul.async Callback]))

(defn host-and-port
  "Construct a com.google.common.net.HostAndPort.

  Arguments may be any of:

  * A HostAndPort instance.
  * A single host string, or a host:port string.
  * A host string, and a port number.
  * A map with keys :host and optionally :port."
  [& args]
  (cond (instance? HostAndPort (first args))
        (first args)

        (and (= 1 (count args)) (string? (first args)))
        (HostAndPort/fromString (first args))

        (and (= 2 (count args)) (string? (first args)) (integer? (second args)))
        (HostAndPort/fromParts (first args) (second args))

        :else
        (let [{:keys [host port]} (apply hash-map args)]
          (if (some? port)
            (HostAndPort/fromParts host port)
            (HostAndPort/fromHost host)))))

(defn cache-config
  "Create a com.orbitz.consul.config.CacheConfig.

  If the sole argument is a CacheConfig, return that argument.

  Otherwise, build a CacheConfig based on keyword keys:

  * :back-off-delay  A sequence of the [min-delay max-delay], or a single [delay] value. Values are duration specs.
  * :min-delay-between-requests The min delay between requests. A duration spec.
  * :timeout-auto-adjusted A boolean.
  * :timeout-auto-adjustment-margin The timeout adjustment margin. A duration spec.
  * :refresh-error-logged-as-warning A boolean.
  * :refresh-error-logged-as-error A boolean.
  * :refresh-error-logged-as A com.orbitz.conul.config.CacheConfig$RefreshErrorLogConsumer instance.

  All duration specs can either be a long (milliseconds), a float (seconds), a pair of [long java.util.concurrent.TimeUnit],
  a java.time.Duration, or a string to parse with java.time.Duration/parse."
  [& args]
  (if (instance? CacheConfig (first args))
    (first args)
    (let [{:keys [back-off-delay min-delay-between-requests timeout-auto-adjusted timeout-auto-adjustment-margin
                  refresh-error-logged-as-warning refresh-error-logged-as-error refresh-error-logged-as]} (apply hash-map (map->seq args))]
      (as-> (CacheConfig/builder) ^CacheConfig$Builder $
            (cond (and (seq back-off-delay) (= 2 (count back-off-delay)))
                  (.withBackOffDelay $ (to-duration (first back-off-delay)) (to-duration (second back-off-delay)))
                  (and (seq back-off-delay) (= 1 (count back-off-delay)))
                  (.withBackOffDelay $ (to-duration (first back-off-delay)))
                  (nil? back-off-delay) $)
            (if (some? min-delay-between-requests) (.withMinDelayBetweenRequests $ (to-duration min-delay-between-requests)) $)
            (if (some? timeout-auto-adjusted) (.withTimeoutAutoAdjustmentEnabled $ (boolean timeout-auto-adjusted)) $)
            (if (some? timeout-auto-adjustment-margin) (.withTimeoutAutoAdjustmentMargin $ (to-duration timeout-auto-adjustment-margin)) $)
            (if refresh-error-logged-as-warning (.withRefreshErrorLoggedAsWarning $) $)
            (if refresh-error-logged-as-error (.withRefreshErrorLoggedAsError $) $)
            (if (some? refresh-error-logged-as) (.withRefreshErrorLoggedAs $ refresh-error-logged-as))
            (.build $)))))

(defn client-configuration
  "Create a com.orbitz.consul.config.ClientConfig.

  If the sole argument is a ClientConfig, return that argument.

  Otherwise, build a ClientConfig based on keyword keys:

  * :cache-config The cache config. See consul.core/cache-config."
  [& args]
  (if (instance? ClientConfig (first args))
    (first args)
    (let [->cache-config cache-config
          {:keys [cache-config]} (apply hash-map (map->seq args))]
      (ClientConfig. (apply ->cache-config cache-config)))))

(defn consul
  "Construct a consul client.

  Passing in an instance of com.orbitz.consul.Consul as the only argument
  returns that argument.

  Otherwise, a new consul client is constructed based on keyword arguments:

  * :url            A java.net.URL, the consul agent URL to use.
  * :ping           A boolean, whether or not to ping the server first.
  * :basic-auth     Basic authentication credentials. This can be a map with keys
                    :username and :password, or a sequence [username, password].
  * :acl-token      An ACL token string.
  * :headers        A map of string->string, headers to add to requests.
  * :consul-bookend A com.orbitz.consul.util.bookend.ConsulBookend instance.
  * :host-and-port  A map of keys (:host, :port), or a string \"host:port\",
                    or a com.google.common.net.HostAndPort instance. The host
                    and port to connect to the consul agent.
  * :ssl-context    A javax.net.ssl.SSLContext instance.
  * :trust-manager  A javax.net.ssl.X509TrustManager instance.
  * :hostname-verifier A javax.net.ssl.HostnameVerifier instance.
  * :proxy          A java.net.Proxy instance.
  * :connect-timeout The connect timeout; this can be a long (milliseconds),
                     a float (seconds), a pair of [long, java.util.concurrent.TimeUnit],
                     a java.time.Duration, or a string (parsed via Duration.parse). The
                     minimum granularity is milliseconds.
  * :read-timeout   The read timeout; see connect-timeout.
  * :write-timeout  The write timeout; see connect-timeout.
  * :executor-service A java.util.concurrent.ExecutorService instance.
  * :client-configuration A com.orbitz.consul.config.ClientConfig instance, or a map
                          that specs out fields of that class. See consul.core/client-configuration.
  * :client-event-callback A com.orbitz.consul.monitoring.ClientEventCallback instance."
  [& args]
  (if (and (= 1 (count args)) (instance? Consul (first args)))
    (first args)
    (let [->host-and-port host-and-port
          ->client-configuration client-configuration
          {:keys [url ping basic-auth acl-token headers consul-bookend host-and-port ssl-context trust-manager
                  hostname-verifier proxy connect-timeout read-timeout write-timeout executor-service
                  client-configuration client-event-callback]} (apply hash-map args)]
      (-> (Consul/builder)
          (with- url .withUrl)
          (with- ping .ping)
          (as-> $ (cond (map? basic-auth) (.withBasicAuth $ (:user basic-auth) (:password basic-auth))
                        (seq basic-auth) (.withBasicAuth $ (first basic-auth) (second basic-auth))
                        (nil? basic-auth) $))
          (with- acl-token .withAclToken)
          (as-> $ (cond (map? headers) (.withHeaders $ headers)
                        (nil? headers) $))
          (with- consul-bookend .withConsulBookend)
          (with- (some->> host-and-port (map->seq) (apply ->host-and-port)) .withHostAndPort)
          (with- ssl-context .withSslContext)
          (with- trust-manager .withTrustManager)
          (with- hostname-verifier .withHostnameVerifier)
          (with- proxy .withProxy)
          (with- (some-> connect-timeout to-millis) .withConnectTimeoutMillis)
          (with- (some-> read-timeout to-millis) .withReadTimeoutMillis)
          (with- (some-> write-timeout to-millis) .withWriteTimeoutMillis)
          (with- executor-service .withExecutorService)
          (with- (some-> client-configuration ->client-configuration) .withClientConfiguration)
          (with- client-event-callback .withClientEventCallback)
          (.build)))))

(extend-protocol IConsul
  Consul
  (agent-client [this] (.agentClient this))
  (acl-client [this] (.aclClient this))
  (catalog-client [this] (.catalogClient this))
  (health-client [this] (.healthClient this))
  (kv-client [this] (.keyValueClient this))
  (status-client [this] (.statusClient this))
  (session-client [this] (.sessionClient this))
  (event-client [this] (.eventClient this))
  (prepared-query-client [this] (.preparedQueryClient this))
  (coordinate-client [this] (.coordinateClient this))
  (operator-client [this] (.operatorClient this))
  (snapshot-client [this] (.statusClient this)))

(defn reg-check
  "Construct a com.orbitz.consul.model.agent.Registration$RegCheck.

  The argument may be an instance of RegCheck, or keyword arguments:

  * :script      The script check string.
  * :interval    The check interval duration string.
  * :ttl         The TTL duration string.
  * :http        The HTTP check string.
  * :tcp         The TCP check string.
  * :grpc        The gRPC check string.
  * :grpc-use-tls? A boolean, whether to use TLS for gRPC checks.
  * :timeout     The timeout duration string.
  * :notes       The notes string.
  * :deregister-critical-services-after A duration string to deregister critical services after.
  * :tls-skip-verify A boolean, whether to skip verification for TLS checks.
  * :status      A status string."
  [& args]
  (if (instance? Registration$RegCheck (first args))
    (first args)
    (let [{:keys [script interval ttl http tcp grpc grpc-use-tls? timeout notes deregister-critical-services-after
                  tls-skip-verify status]} (apply hash-map args)]
      (-> (ImmutableRegCheck/builder) ^ImmutableRegCheck$Builder
          (with- script .script)
          (with- interval .interval)
          (with- ttl .ttl)
          (with- http .http)
          (with- tcp .tcp)
          (with- grpc .grpc)
          (with- grpc-use-tls? .grpcUseTls)
          (with- timeout .timeout)
          (with- notes .notes)
          (with- deregister-critical-services-after .deregisterCriticalServiceAfter)
          (with- tls-skip-verify .tlsSkipVerify)
          (with- status .status)
          (.build)))))

(defn registration
  "Construct a com.orbitz.consul.model.agent.Registration.

  The argument may be an instance of Registration, or keyword arguments.

  * :name        The service name string.
  * :id          The optional service ID string.
  * :address     The service address.
  * :port        The service port.
  * :checks      A sequence of checks; each element may be a RegCheck instance, or
                 a map containing keys as described in consul.core/reg-check.
  * :tags        A sequence of tag strings.
  * :meta        A map of metadata (string->string).
  * :enable-tag-override A boolean."
  [& args]
  (if (instance? Registration (first args))
    (first args)
    (let [{:keys [name id address port checks tags meta enable-tag-override]} (apply hash-map args)]
      (-> (ImmutableRegistration/builder) ^ImmutableRegistration$Builder
          (with- name .name)
          (with- id .id)
          (with- address .address)
          (with- port .port)
          (with- (not-empty (map #(apply reg-check (map->seq %)) checks)) .checks)
          (with- tags .tags)
          (with- meta .meta)
          (with- enable-tag-override .enableTagOverride)
          (.build)))))

(defn consistency-mode
  "Coerce the argument to a com.orbitz.consul.option.ConsistencyMode."
  [v]
  (cond (instance? ConsistencyMode v)
        v

        (string? v)
        (ConsistencyMode/valueOf ConsistencyMode (string/upper-case v))

        (keyword? v)
        (consistency-mode (name v))

        (nil? v) nil))

(defn query-options
  "Construct a com.orbitz.consul.option.QueryOptions.

  The argument may be a QueryOptions instance, or keyword keys:

  * :wait        The wait duration string (e.g. 1m, 30s).
  * :token       The token string.
  * :index       A BigInteger index.
  * :near        The near string.
  * :datacenter  The datacenter string.
  * :node-meta   The node metadata map.
  * :tags        The list of service tags.
  * :consistency-mode A string, keyword, or ConsistencyMode value."
  [& args]
  (if (instance? QueryOptions (first args))
    (first args)
    (let [->cm consistency-mode
          {:keys [wait token index near datacenter node-meta tags consistency-mode]} (apply hash-map args)]
      (-> (ImmutableQueryOptions/builder) ^ImmutableQueryOptions$Builder
          (with- wait .wait)
          (with- token .token)
          (with- index .index)
          (with- near .near)
          (with- datacenter .datacenter)
          (with- node-meta .nodeMeta)
          (with- tags .tag)
          (with- (->cm consistency-mode) .consistencyMode)
          (.build)))))

(declare check)

(extend AgentClient
  agent/IAgent
  {:registered? (fn registered? [this service] (.isRegistered this service))
   :ping (fn ping [this] (.ping this))
   :register! (fn register!
                ([this args] (.register this (apply registration (map->seq args))))
                ([this args opts] (.register this (apply registration (map->seq args)) (apply query-options (map->seq opts)))))
   :deregister! (fn deregister!
                  ([this service-id] (.deregister this service-id))
                  ([this service-id opts] (.deregister this service-id (apply query-options (map->seq opts)))))
   :register-check! (fn register-check!
                      [this args]
                      (.registerCheck this (apply check (map->seq args))))
   :deregister-check! (fn deregister-check!
                        [this check-id]
                        (.deregisterCheck this check-id))
   :agent (fn agent [this] (to-bean-map (.getAgent this)))
   :checks (fn checks [this] (into {} (map (fn [[k v]] (vector k (to-bean-map v))) (.getChecks this))))
   :services (fn services [this] (into {} (map (fn [[k v]] (vector k (to-bean-map v))) (.getServices this))))
   :members (fn members [this] (map to-bean-map (.getMembers this)))
   :force-leave! (fn force-leave! [this node-id] (.forceLeave this node-id))
   :check! (fn check! [this check-id state note] (.check this check-id state note))
   :check-ttl! (fn check-ttl! [this service-id state note] (.checkTtl this service-id state note))
   :pass! (fn pass!
            ([this service-id] (.pass this service-id))
            ([this service-id note] (.pass this service-id note)))
   :warn! (fn warn!
            ([this service-id] (.warn this service-id))
            ([this service-id note] (.warn this service-id note)))
   :fail! (fn fail!
            ([this service-id] (.fail this service-id))
            ([this service-id note] (.fail this service-id note)))
   :pass-check! (fn pass-check!
                  ([this check-id] (.passCheck this check-id))
                  ([this check-id note] (.passCheck this check-id note)))
   :warn-check! (fn warn-check!
                  ([this check-id] (.warnCheck this check-id))
                  ([this check-id note] (.warnCheck this check-id note)))
   :fail-check! (fn fail-check!
                  ([this check-id] (.failCheck this check-id))
                  ([this check-id note] (.failCheck this check-id note)))
   :join! (fn join!
            ([this address] (.join this address))
            ([this address wan?] (.join this address wan?)))
   :maintenance-mode! (fn maintenance-mode!
                        ([this service-id enable?] (.toggleMaintenanceMode this service-id enable?))
                        ([this service-id enable? reason] (.toggleMaintenanceMode this service-id enable? reason)))})

(defn tagged-addresses
  "Construct a com.orbitz.consul.model.catalog.TaggedAddresses.

  The arguments may be a TaggedAddresses instance, or keyword arguments:

  * :lan   The LAN address string.
  * :wan   The WAN address string."
  [& args]
  (if (instance? TaggedAddresses (first args))
    (first args)
    (let [{:keys [lan wan]} (apply hash-map args)]
      (-> (ImmutableTaggedAddresses/builder)
          (with- lan .lan)
          (with- wan .wan)
          (.build)))))

(defn check
  "Construct a com.orbitz.consul.model.agent.Check.

  The arguments may be a single Check instanec, or keyword arguments:

  * :id          The check ID string.
  * :name        The check name string.
  * :notes       The check notes string.
  * :output      The check output string.
  * :script      The check script string.
  * :interval    The check interval duration string.
  * :ttl         The check TTL duration string.
  * :http        The HTTP check string.
  * :tcp         The TCP check string.
  * :grpc        The gRPC check string.
  * :grpc-use-tls? A boolean, whether to use TLS for gRPC checks.
  * :service-id  The service ID string.
  * :service-tags A sequence of service tag strings.
  * :deregister-critical-service-after A duration string."
  [& args]
  (if (instance? Check (first args))
    (first args)
    (let [{:keys [id name notes output script interval ttl http tcp grpc grpc-use-tls? service-id service-tags deregister-critical-service-after]}
          (apply hash-map args)]
      (-> (ImmutableCheck/builder)
          (with- id .id)
          (with- name .name)
          (with- notes .notes)
          (with- output .output)
          (with- script .script)
          (with- interval .interval)
          (with- ttl .ttl)
          (with- http .http)
          (with- tcp .tcp)
          (with- grpc .grpc)
          (with- grpc-use-tls? .grpcUseTls)
          (with- service-id .serviceId)
          (with- service-tags .serviceTags)
          (with- deregister-critical-service-after .deregisterCriticalServiceAfter)
          (.build)))))

(defn write-request
  "Construct a com.orbitz.consul.model.catalog.WriteRequest.

  The arguments may be a single WriteRequest instance, or keyword keys:

  * :token       The write request token string."
  [& args]
  (if (instance? WriteRequest (first args))
    (first args)
    (let [{:keys [token]} (apply hash-map args)]
      (-> (ImmutableWriteRequest/builder)
          (with- token .token)
          (.build)))))

(defn catalog-registration
  "Construct a com.orbitz.consul.model.catalog.CatalogRegistration.

  The arguments may be a single CatalogRegistration instance"
  [& args]
  (if (instance? CatalogRegistration (first args))
    (first args)
    (let [->ta tagged-addresses
          ->check check
          ->wr write-request
          {:keys [datacenter node address tagged-addresses service check write-request]}
          (apply hash-map args)]
      (-> (ImmutableCatalogRegistration/builder)
          (with- datacenter .datacenter)
          (with- node .node)
          (with- address .address)
          (with- (when (some? tagged-addresses) (apply ->ta (map->seq tagged-addresses))) .taggedAddresses)
          (with- service .service)
          (with- (when (some? check) (apply ->check (map->seq check))) .check)
          (with- (when (some? write-request) (apply ->wr write-request)) .writeRequest)
          (.build)))))

(defn ^:no-doc catalog-deregistration
  [& args]
  (if (instance? CatalogDeregistration (first args))
    (first args)
    (let [->wr write-request
          {:keys [datacenter node check-id service-id write-request]} (apply hash-map args)]
      (-> (ImmutableCatalogDeregistration/builder)
          (with- datacenter .datacenter)
          (with- node .node)
          (with- check-id .checkId)
          (with- service-id .serviceId)
          (with- (some-> write-request ->wr) .writeRequest)
          (.build)))))

(extend CatalogClient
  catalog/ICatalog
  {:datacenters (fn datacenters [this] (.getDatacenters this))
   :nodes (fn nodes ([this] (to-bean-map (.getNodes this)))
                    ([this opts] (to-bean-map (.getNodes this (apply query-options (map->seq opts))))))
   :services (fn services ([this] (to-bean-map (.getServices this)))
                          ([this opts] (to-bean-map (.getServices this (apply query-options (map->seq opts))))))
   :service (fn service ([this service-name] (to-bean-map (.getService this service-name)))
                        ([this service-name opts] (to-bean-map (.getService this service-name (apply query-options (map->seq opts))))))
   :node (fn node ([this node-name] (to-bean-map (.getNode this node-name)))
                  ([this node-name opts] (to-bean-map (.getNode this node-name (apply query-options (map->seq opts))))))
   :register! (fn register! ([this args] (.register this (apply catalog-registration (map->seq args))))
                            ([this args opts] (.register this (apply catalog-registration (map->seq args)) (apply query-options (map->seq opts)))))
   :deregister! (fn deregister! ([this args] (.deregister this (apply catalog-deregistration (map->seq args))))
                                ([this args opts] (.deregister this (apply catalog-deregistration (map->seq args)) (apply query-options (map->seq opts)))))})

(defn ^:no-doc acl-token
  [& args]
  (if (instance? AclToken (first args))
    (first args)
    (let [{:keys [id name type rules]} (apply hash-map args)]
      (-> (ImmutableAclToken/builder)
          (with- id .id)
          (with- name .name)
          (with- type .type)
          (with- rules .rules)
          (.build)))))

(extend AclClient
  acl/IAcl
  {:create-acl (fn create-acl [this args] (.createAcl this (apply acl-token (map->seq args))))
   :update-acl (fn update-acl [this args] (.updateAcl this (apply acl-token (map->seq args))))
   :destroy-acl (fn destroy-acl [this id] (.destroyAcl this id))
   :acl-info (fn acl-info [this id] (to-bean-map (.getAclInfo this id)))
   :clone-acl (fn clone-acl [this id] (.cloneAcl this id))
   :list-acls (fn list-acls [this] (to-bean-map (.listAcls this)))})

(defn ^:no-doc ->state
  [s]
  (cond (instance? State s) s
        (string? s) (State/fromName s)))

(extend HealthClient
  health/IHealth
  {:node-checks (fn node-checks
                  ([this node] (to-bean-map (.getNodeChecks this node)))
                  ([this node opts] (to-bean-map (.getNodeChecks this node (apply query-options (map->seq opts))))))
   :service-checks (fn service-checks
                     ([this service] (to-bean-map (.getServiceChecks this service)))
                     ([this service opts] (to-bean-map (.getServiceChecks this service (apply query-options (map->seq opts))))))
   :checks-by-state (fn checks-by-state
                      ([this state] (to-bean-map (.getChecksByState this (->state state))))
                      ([this state opts] (to-bean-map (.getChecksByState this (->state state) (apply query-options (map->seq opts))))))
   :healthy-service-instances (fn healthy-service-instances
                                ([this service] (to-bean-map (.getHealthyServiceInstances this service)))
                                ([this service opts] (to-bean-map (.getHealthyServiceInstances this service (apply query-options (map->seq opts))))))
   :service-instances (fn service-instances
                        ([this service] (to-bean-map (.getAllServiceInstances this service)))
                        ([this service opts] (to-bean-map (.getAllServiceInstances this service (apply query-options (map->seq opts))))))})

(defn ^:no-doc ->charset
  [v]
  (cond (nil? v) nil
        (instance? Charset v) v
        (string? v) (Charset/forName v)))

(defn operation
  "Construct a com.orbitz.consul.model.kv.Operation.

  If passed a single Operation argument, return that argument.
  Otherwise interpret the keyword arguments:

  * :verb       The verb string.
  * :key        The key string.
  * :value      The value string.
  * :flags      A long bitset of flags.
  * :index      The BigInteger index value.
  * :session    The session string."
  [& args]
  (if (instance? Operation (first args))
    (first args)
    (let [{:keys [verb key value flags index session]} (apply hash-map args)]
      (-> (ImmutableOperation/builder)
          (with- verb .verb)
          (with- key .key)
          (with- value .value)
          (with- flags .flags)
          (with- index .index)
          (with- session .session)
          (.build)))))

(defn transaction-options
  "Construct a com.orbitz.consul.option.TransactionOptions.

  If passed a single TransactionOptions argument, return that argument.
  Otherwise interpret the keyword arguments:

  * :datacenter       The datacenter string.
  * :consistency-mode A ConsistencyMode instance, or a string consistency mode to parse."
  [& args]
  (if (instance? TransactionOptions (first args))
    (first args)
    (let [->consistency-mode consistency-mode
          {:keys [datacenter consistency-mode]} (apply hash-map args)]
      (-> (ImmutableTransactionOptions/builder)
          (with- datacenter .datacenter)
          (with- (some-> consistency-mode ->consistency-mode) .consistencyMode)
          (.build)))))

(extend KeyValueClient
  kv/IKeyValue
  {:get-value (fn get-value
                ([this key] (to-bean-map (.getValue this key)))
                ([this key opts] (to-bean-map (.getValue this key (apply query-options (map->seq opts))))))
   :get-values (fn get-values
                 ([this key] (to-bean-map (.getValues this key)))
                 ([this key opts] (to-bean-map (.getValues this key (apply query-options (map->seq opts))))))
   :put-value! (fn put-value!
                 ([this key value] (to-bean-map (.putValue this key value)))
                 ([this key value opts] (let [flags (:flags opts)
                                              charset (->charset (:charset opts))]
                                          (to-bean-map (.putValue this key value (or flags 0) (apply query-options (map->seq opts)) charset)))))
   :get-keys (fn get-keys
               [this key] (.getKeys this key))
   :delete-key! (fn delete-key!
                  [this key] (.deleteKey this key))
   :delete-keys! (fn delete-keys!
                   [this key] (.deleteKeys this key))
   :acquire-lock (fn acquire-lock
                   ([this key session] (.acquireLock this key session))
                   ([this key session value] (.acquireLock this key session value)))
   :get-session (fn get-session
                  [this key] (.getSession this key))
   :release-lock (fn release-lock
                   [this key session] (.releaseLock this key session))
   :transact! (fn transact!
                ([this operations] (to-bean-map (.performTransaction this (into-array Operation (map #(apply operation (map->seq %)) operations)))))
                ([this operations options] (to-bean-map (.performTransaction this (apply transaction-options (map->seq options)) (into-array Operation (map #(apply operation (map->seq %)) operations))))))})

(extend StatusClient
  status/IStatus
  {:leader (fn [this] (.getLeader this))
   :peers (fn [this] (.getPeers this))})

(defn ^:no-doc session
  [& args]
  (if (instance? Session (first args))
    (first args)
    (let [{:keys [lock-delay name node checks behavior ttl]} (apply hash-map args)]
      (-> (ImmutableSession/builder)
          (with- lock-delay .lockDelay)
          (with- name .name)
          (with- node .node)
          (with- checks .checks)
          (with- behavior .behavior)
          (with- ttl .ttl)
          (.build)))))

(extend SessionClient
  session/ISession
  {:create-session (fn create-session
                     ([this value] (to-bean-map (.createSession this (apply session (map->seq value)))))
                     ([this value dc] (to-bean-map (.createSession this (apply session (map->seq value)) dc))))
   :renew-session (fn renew-session
                    ([this id] (to-bean-map (.renewSession this id)))
                    ([this id dc] (to-bean-map (.renewSession this id dc))))
   :destroy-session (fn destroy-session
                      ([this id] (.destroySession this id))
                      ([this id dc] (.destroySession this id dc)))
   :session-info (fn session-info
                   ([this id] (to-bean-map (.getSessionInfo this id)))
                   ([this id dc] (to-bean-map (.getSessionInfo this id dc))))
   :list-sessions (fn list-sessions
                    ([this] (to-bean-map (.listSessions this)))
                    ([this dc] (to-bean-map (.listSessions this dc))))})

(defn ^:no-doc event-options
  [& args]
  (if (instance? EventOptions (first args))
    (first args)
    (let [{:keys [datacenter node-filter service-filter tag-filter]} (apply hash-map args)]
      (-> (ImmutableEventOptions/builder)
          (with- datacenter .datacenter)
          (with- node-filter .nodeFilter)
          (with- service-filter .serviceFilter)
          (with- tag-filter .tagFilter)
          (.build)))))

(extend EventClient
  event/IEvent
  {:fire-event! (fn fire-event!
                  ([this name] (to-bean-map (.fireEvent this name)))
                  ([this name opts] (to-bean-map (.fireEvent this name (apply event-options (map->seq opts)))))
                  ([this name opts payload] (to-bean-map (.fireEvent this name (apply event-options (map->seq opts)) payload))))
   :list-events (fn list-events
                  ([this] (to-bean-map (.listEvents this)))
                  ([this name-or-opts]
                   (to-bean-map (if (string? name-or-opts)
                                  (.listEvents this name-or-opts)
                                  (.listEvents this (apply query-options (map->seq name-or-opts))))))
                  ([this name opts]
                   (to-bean-map (.listEvents this name (apply query-options (map->seq opts))))))})

(defn ^:no-doc template
  [& args]
  (if (instance? Template (first args))
    (first args)
    (let [{:keys [type regex]} (apply hash-map args)]
      (-> (ImmutableTemplate/builder)
          (with- type .type)
          (with- regex .regExp)
          (.build)))))

(defn ^:no-doc dns-query
  [& args]
  (if (instance? DnsQuery (first args))
    (first args)
    (let [{:keys [ttl]} (apply hash-map args)]
      (-> (ImmutableDnsQuery/builder)
          (with- ttl .ttl)
          (.build)))))

(defn ^:no-doc prepared-query
  [& args]
  (if (instance? PreparedQuery (first args))
    (first args)
    (let [->template template
          {:keys [template name session token service dns]} (apply hash-map args)]
      (-> (ImmutablePreparedQuery/builder)
          (with- (some->> template (apply ->template)) .template)
          (with- name .name)
          (with- session .session)
          (with- token .token)
          (with- service .service)
          (with- (some->> dns (apply dns-query)) .dns)
          (.build)))))

(extend PreparedQueryClient
  pq/IPreparedQuery
  {:prepare-query! (fn prepare-query!
                     ([this query] (.createPreparedQuery this (apply prepared-query (map->seq query))))
                     ([this query dc] (.createPreparedQuery this (apply prepared-query (map->seq query)) dc)))
   :prepared-queries (fn prepared-queries
                       ([this] (to-bean-map (.getPreparedQueries this)))
                       ([this dc] (to-bean-map (.getPreparedQueries this dc))))
   :prepared-query (fn prepared-query
                     ([this id] (to-bean-map (.getPreparedQuery this id)))
                     ([this id dc] (to-bean-map (.getPreparedQuery this id dc))))
   :execute! (fn execute! [this name-or-id] (to-bean-map (.execute this name-or-id)))})

(extend CoordinateClient
  coordinate/ICoordinate
  {:datacenters (fn datacenters [this] (to-bean-map (.getDatacenters this)))
   :nodes (fn nodes
            ([this] (to-bean-map (.getNodes this)))
            ([this dc] (to-bean-map (.getNodes this dc))))})

(extend OperatorClient
  operator/IOperator
  {:raft-config (fn raft-config
                  ([this] (to-bean-map (.getRaftConfiguration this)))
                  ([this dc] (to-bean-map (.getRaftConfiguration this dc))))
   :stale-raft-config (fn stale-raft-config
                        ([this] (to-bean-map (.getStaleRaftConfiguration this)))
                        ([this dc] (to-bean-map (.getStaleRaftConfiguration this dc))))
   :delete-peer! (fn delete-peer!
                   ([this address] (.deletePeer this address))
                   ([this address dc] (.deletePeer this address dc)))})

(extend SnapshotClient
  snapshot/ISnapshot
  {:save! (fn save!
            [this dest query-opts]
            (let [p (promise)]
              (.save this (io/file dest) (apply query-options (map->seq query-opts))
                     (reify Callback
                       (onResponse [_ result] (deliver p result))
                       (onFailure [_ result] (deliver p result))))
              p))
   :restore! (fn restore!
               [this source query-opts]
               (let [p (promise)]
                 (.restore this (io/file source)
                           (apply query-options (map->seq query-opts))
                           (reify Callback
                             (onResponse [_ r] (deliver p r))
                             (onFailure [_ r] (deliver p r))))
                 p))})
