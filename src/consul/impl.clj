(ns consul.impl
  (:import [java.time Duration]
           [java.util.concurrent TimeUnit]
           [java.util Optional Map]))

(defprotocol Beanable
  (to-bean-map [this] "Coerce this, a java Bean, into a map. So like clojure.core/bean, but recursive in the right ways."))

(extend-protocol Beanable
  nil
  (to-bean-map [this] this)

  Object
  (to-bean-map [this] this)

  Iterable
  (to-bean-map [this] (map to-bean-map this))

  Map
  (to-bean-map [this] (into {} (map (fn [[k v]] [(to-bean-map k) (to-bean-map v)]) this)))

  Optional
  (to-bean-map [this] (if (.isPresent this) (to-bean-map (.get this)) nil))

  com.orbitz.consul.model.agent.Agent
  (to-bean-map [this] {:config (when-let [c (.getConfig this)]
                                 (bean c))
                       :debugConfig (when-let [c (.getDebugConfig this)]
                                      (bean c))
                       :member (when-let [c (.getMember this)]
                                 (bean c))})

  com.orbitz.consul.model.ConsulResponse
  (to-bean-map [this] (update (bean this) :response to-bean-map))

  com.orbitz.consul.model.health.Node
  (to-bean-map [this] (into {} (map #(vector (key %) (to-bean-map (val %))) (bean this))))

  com.orbitz.consul.model.catalog.TaggedAddresses
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.acl.AclResponse
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.health.HealthCheck
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.health.ServiceHealth
  (to-bean-map [this] (update (bean this) :checks to-bean-map))

  com.orbitz.consul.model.kv.Value
  (to-bean-map [this] (->> (bean this)
                           (map (fn [[k v]] [k (to-bean-map v)]))
                           (into {})))

  com.orbitz.consul.model.kv.TxResponse
  (to-bean-map [this] {:results (to-bean-map (.results this))
                       :errors (to-bean-map (.errors this))})

  com.orbitz.consul.model.kv.TxError
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.session.SessionCreatedResponse
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.session.SessionInfo
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.event.Event
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.query.StoredQuery
  (to-bean-map [this] (-> (bean this)
                          (update :dns to-bean-map)
                          (update :service to-bean-map)))

  com.orbitz.consul.model.query.DnsQuery
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.query.ServiceQuery
  (to-bean-map [this] (update (bean this) :failover to-bean-map))

  com.orbitz.consul.model.query.QueryResults
  (to-bean-map [this] (update (bean this) :nodes to-bean-map))

  com.orbitz.consul.model.operator.RaftConfiguration
  (to-bean-map [this] (update (bean this) :servers to-bean-map))

  com.orbitz.consul.model.operator.RaftServer
  (to-bean-map [this] (bean this))

  com.orbitz.consul.model.health.Service
  (to-bean-map [this] (bean this)))

(defn to-millis
  [v]
  (cond (nil? v)
        0

        (integer? v)
        v

        (float? v)
        (long (* v 1000))

        (instance? Duration v)
        (.toMillis v)

        (string? v)
        (.toMillis (Duration/parse v))

        (and (seq v) (integer? (first v)) (instance? TimeUnit (second v)))
        (.convert TimeUnit/MILLISECONDS (first v) (second v))

        :else
        (throw (IllegalArgumentException. (str "can't convert value of type " (type v) " to milliseconds")))))

(defn to-duration
  [v]
  (cond (nil? v)
        (Duration/ofNanos 0)

        (integer? v)
        (Duration/ofMillis v)

        (float? v)
        (Duration/ofMillis (long (* v 1000)))

        (string? v)
        (Duration/parse v)

        (and (seq v) (integer? (first v)) (instance? TimeUnit (second v)))
        (Duration/ofMillis (.convert TimeUnit/MILLISECONDS (first v) (second v)))

        :else
        (throw (IllegalArgumentException. (str "can't convert value of type " (type v) " to Duration")))))

(defmacro with-
  [this v setter]
  `((fn [this#]
     (let [v# ~v]
       (if (some? v#) (~setter this# v#) this#)))
    ~this))

(defn map->seq
  [maybe-map]
  (if (map? maybe-map)
    (flatten (seq maybe-map))
    maybe-map))