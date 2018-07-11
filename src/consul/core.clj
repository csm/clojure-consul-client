(ns consul.core
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
  [& args]
  (println "host-and-port:" args)
  (cond (instance? HostAndPort (first args))
        (first args)

        (and (= 1 (count args)) (string? (first args)))
        (HostAndPort/fromString (first args))

        (and (= 2 (count args)) (string? (first args)) (integer? (second args)))
        (HostAndPort/fromParts (first args) (second args))

        :else
        (let [[& {:keys [host port]}] (apply hash-map (map->seq args))]
          (if (some? port)
            (HostAndPort/fromParts host port)
            (HostAndPort/fromHost host)))))

(defn cache-config
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
  [& args]
  (if (instance? ClientConfig (first args))
    (first args)
    (let [->cache-config cache-config
          {:keys [cache-config]} (apply hash-map (map->seq args))]
      (ClientConfig. (apply ->cache-config cache-config)))))

(defn consul
  [& args]
  (if (and (= 1 (count args)) (instance? Consul (first args)))
    (first args)
    (let [->host-and-port host-and-port
          ->client-configuration client-configuration
          _ (println "args are:" args)
          {:keys [url ping basicAuth acl-token headers consul-bookend host-and-port ssl-context trust-manager
                  hostname-verifier proxy connect-timeout read-timeout write-timeout executor-service
                  client-configuration client-event-callback]} (apply hash-map args)]
      (-> (Consul/builder)
          (with- url .withUrl)
          (with- ping .ping)
          (as-> $ (cond (map? basicAuth) (.withBasicAuth $ (:user basicAuth) (:password basicAuth))
                        (seq basicAuth) (.withBasicAuth $ (first basicAuth) (second basicAuth))
                        (nil? basicAuth) $))
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
  [& args]
  (if (instance? Registration (first args))
    (first args)
    (let [{:keys [name id address port checks tags meta enable-tag-override]} (apply hash-map args)]
      (-> (ImmutableRegistration/builder) ^ImmutableRegistration$Builder
          (with- name .name)
          (with- id .id)
          (with- address .address)
          (with- port .port)
          (with- (not-empty (map #(apply reg-check %) checks)) .checks)
          (with- tags .tags)
          (with- meta .meta)
          (with- enable-tag-override .enableTagOverride)
          (.build)))))

(defn consistency-mode
  [v]
  (cond (instance? ConsistencyMode v)
        v

        (string? v)
        (ConsistencyMode/valueOf ConsistencyMode (string/upper-case v))

        (keyword? v)
        (consistency-mode (name v))

        (nil? v) nil))

(defn query-options
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

(extend-protocol agent/IAgent
  AgentClient
  (registered? [this service] (.isRegistered this service))
  (ping [this] (.ping this))
  (register! [this args] (.register this (apply registration (map->seq args))))
  (register! [this args opts] (.register this (apply registration (map->seq args)) (apply query-options (map->seq opts))))
  (deregister! [this service-id] (.deregister this service-id))
  (deregister! [this service-id opts] (.deregister this service-id (apply query-options (map->seq opts))))
  (agent [this] (to-bean-map (.getAgent this)))
  (checks [this] (into {} (map (fn [[k v]] (vector k (to-bean-map v))) (.getChecks this))))
  (services [this] (into {} (map (fn [[k v]] (vector k (to-bean-map v))) (.getServices this))))
  (members [this] (map to-bean-map (.getMembers this)))
  (force-leave! [this node-id] (.forceLeave this node-id))
  (check! [this check-id state note] (.check this check-id state note))
  (check-ttl! [this service-id state note] (.checkTtl this service-id state note))
  (pass! [this service-id] (.pass this service-id))
  (pass! [this service-id note] (.pass this service-id note))
  (warn! [this service-id] (.warn this service-id))
  (warn! [this service-id note] (.warn this service-id note))
  (fail! [this service-id] (.fail this service-id))
  (fail! [this service-id note] (.fail this service-id note))
  (pass-check! [this check-id] (.passCheck this check-id))
  (pass-check! [this check-id note] (.passCheck this check-id note))
  (warn-check! [this check-id] (.warnCheck this check-id))
  (warn-check! [this check-id note] (.warnCheck this check-id note))
  (fail-check! [this check-id] (.failCheck this check-id))
  (fail-check! [this check-id note] (.failCheck this check-id note))
  (join! [this address] (.join this address))
  (join! [this address wan?] (.join this address wan?))
  (maintenance-mode! [this service-id enable?] (.toggleMaintenanceMode this service-id enable?))
  (maintenance-mode! [this service-id enable? reason] (.toggleMaintenanceMode this service-id enable? reason)))

(defn tagged-addresses
  [& args]
  (if (instance? TaggedAddresses (first args))
    (first args)
    (let [{:keys [lan wan]} (apply hash-map args)]
      (-> (ImmutableTaggedAddresses/builder)
          (with- lan .lan)
          (with- wan .wan)
          (.build)))))

(defn check
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
  [& args]
  (if (instance? WriteRequest (first args))
    (first args)
    (let [{:keys [token]} (apply hash-map args)]
      (-> (ImmutableWriteRequest/builder)
          (with- token .token)
          (.build)))))

(defn catalog-registration
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

(defn catalog-deregistration
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

(defn acl-token
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

(defn ->state
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

(defn ->charset
  [v]
  (cond (nil? v) nil
        (instance? Charset v) v
        (string? v) (Charset/forName v)))

(defn operation
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

(defn session
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

(defn event-options
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

(defn template
  [& args]
  (if (instance? Template (first args))
    (first args)
    (let [{:keys [type regex]} (apply hash-map args)]
      (-> (ImmutableTemplate/builder)
          (with- type .type)
          (with- regex .regExp)
          (.build)))))

(defn dns-query
  [& args]
  (if (instance? DnsQuery (first args))
    (first args)
    (let [{:keys [ttl]} (apply hash-map args)]
      (-> (ImmutableDnsQuery/builder)
          (with- ttl .ttl)
          (.build)))))

(defn prepared-query
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
  {:datacenters (fn datacenters [this] (.getDatacenters this))
   :nodes (fn nodes
            ([this] (.getNodes this))
            ([this dc] (.getNodes this dc)))})

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
