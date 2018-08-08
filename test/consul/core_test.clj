(ns consul.core-test
  (:require [clojure.test :refer :all]
            [consul.core :refer :all])
  (:import [com.google.common.net HostAndPort]
           [com.orbitz.consul.config CacheConfig ClientConfig]
           [java.time Duration]
           [java.util.concurrent TimeUnit]
           [com.orbitz.consul.model.agent Registration$RegCheck Registration Check]
           [java.util Optional]
           [com.orbitz.consul.option QueryOptions ConsistencyMode]
           [com.orbitz.consul.model.catalog TaggedAddresses]))

(deftest test-host-and-port
  (testing "host-and-port"
    (is (= (HostAndPort/fromHost "localhost")
           (host-and-port "localhost")))
    (is (= (HostAndPort/fromHost "localhost")
           (host-and-port :host "localhost")))
    (is (= (HostAndPort/fromParts "localhost" 1234)
           (host-and-port "localhost" 1234)))
    (is (= (HostAndPort/fromParts "localhost" 1234)
           (host-and-port :host "localhost" :port 1234)))
    (is (= (HostAndPort/fromParts "localhost" 1234)
           (host-and-port "localhost:1234")))))

(deftest test-cache-config
  (testing "consul CacheConfig"
    (let [model (-> (CacheConfig/builder)
                    (.withBackOffDelay (Duration/ofSeconds 10))
                    (.withMinDelayBetweenRequests (Duration/ofSeconds 20))
                    (.withTimeoutAutoAdjustmentEnabled true)
                    (.withRefreshErrorLoggedAsError)
                    (.build))
          ; CacheConfig doesn't override equals.
          eq? (fn [^CacheConfig a ^CacheConfig b]
                (and (= (.getMinimumBackOffDelay a) (.getMaximumBackOffDelay b))
                     (= (.getMaximumBackOffDelay a) (.getMaximumBackOffDelay b))
                     (= (.getMinimumDurationBetweenRequests a) (.getMinimumDurationBetweenRequests b))
                     (= (.isTimeoutAutoAdjustmentEnabled a) (.isTimeoutAutoAdjustmentEnabled b))))]
      (is (eq? model
               (cache-config :back-off-delay 10000
                             :min-delay-between-requests 20000
                             :timeout-auto-adjusted true
                             :refresh-error-logged-as-error true)))
      (is (eq? model
               (cache-config :back-off-delay 10.0
                             :min-delay-between-requests 20.0
                             :timeout-auto-adjusted true
                             :refresh-error-logged-as-error true)))
      (is (eq? model
               (cache-config :back-off-delay [10 TimeUnit/SECONDS]
                             :min-delay-between-requests [20 TimeUnit/SECONDS]
                             :timeout-auto-adjusted true
                             :refresh-error-logged-as-error true)))
      (is (eq? model
               (cache-config :back-off-delay "PT10S"
                             :min-delay-between-requests "PT20S"
                             :timeout-auto-adjusted true
                             :refresh-error-logged-as-error true))))))

(deftest test-client-configuration
  (testing "consul ClientConfig"
    (let [config (client-configuration :cache-config {:back-off-delay 10000
                                                      :min-delay-between-requests 20000
                                                      :timeout-auto-adjusted true
                                                      :refresh-error-logged-as-error true})]
      (is (instance? ClientConfig config))
      (is (= (Duration/ofSeconds 10)) (-> config .getCacheConfig .getMinimumBackOffDelay))
      (is (= (Duration/ofSeconds 10)) (-> config .getCacheConfig .getMaximumBackOffDelay))
      (is (= (Duration/ofSeconds 20)) (-> config .getCacheConfig .getMinimumDurationBetweenRequests))
      (is (true? (-> config .getCacheConfig .isTimeoutAutoAdjustmentEnabled))))))

(deftest test-reg-check
  (testing "consul Registration$RegCheck"
    (let [check (reg-check :interval "1m" :http "http://localhost:1234/")]
      (is (instance? Registration$RegCheck check))
      (is (= (Optional/of "1m") (.getInterval check)))
      (is (= (Optional/of "http://localhost:1234/") (.getHttp check))))
    (let [check (reg-check :interval "1m" :tcp "localhost:1234")]
      (is (instance? Registration$RegCheck check))
      (is (= (Optional/of "1m") (.getInterval check)))
      (is (= (Optional/of "localhost:1234") (.getTcp check))))
    (is (thrown? IllegalStateException (reg-check)))
    (is (thrown? IllegalStateException (reg-check :http "http://localhost:1234/")))))

(deftest test-registration
  (testing "consul Registration"
    (is (thrown? IllegalStateException (registration)))
    (is (thrown? IllegalStateException (registration :name "test")))
    (is (thrown? IllegalStateException (registration :id "test")))
    (let [reg (registration :name "test" :id "test")]
      (is (instance? Registration reg))
      (is (= "test" (.getName reg)))
      (is (= "test" (.getId reg))))
    (is (thrown? IllegalStateException (registration :name "test" :id "test" :checks [{:http "http://localhost:1234/"}])))))

(deftest test-query-options
  (testing "consul QueryOptions"
    (is (thrown? IllegalArgumentException (query-options :wait "1m")))
    (let [opts (query-options :wait "1m" :index (biginteger 1234) :token "token" :near "somewhere" :datacenter "dc1"
                              :node-meta ["foo" "bar"] :tags ["tag1"] :consistency-mode "STALE")]
      (is (instance? QueryOptions opts))
      (is (true? (.isBlocking opts)))
      (is (= (.getWait opts) (Optional/of "1m")))
      (is (= (.getIndex opts) (Optional/of (biginteger 1234))))
      (is (= (.getToken opts) (Optional/of "token")))
      (is (= (.getNear opts) (Optional/of "somewhere")))
      (is (= (.getDatacenter opts) (Optional/of "dc1")))
      (is (= (.getNodeMeta opts) ["foo" "bar"]))
      (is (= (.getTag opts) ["tag1"]))
      (is (= (.getConsistencyMode opts) ConsistencyMode/STALE)))))

(deftest test-tagged-addresses
  (testing "consul TaggedAddresses"
    (is (thrown? IllegalStateException (tagged-addresses)))
    (is (thrown? IllegalStateException (tagged-addresses :lan "1.1.1.1")))
    (is (thrown? IllegalStateException (tagged-addresses :wan "1.1.1.1")))
    (let [addr (tagged-addresses :lan "1.1.1.1" :wan "2.2.2.2")]
      (is (instance? TaggedAddresses addr))
      (is (= (.getLan addr) "1.1.1.1"))
      (is (= (.getWan addr) "2.2.2.2")))))

(deftest test-check
  (testing "consul Check"
    (is (thrown? IllegalStateException (check)))
    (is (thrown? IllegalStateException (check :id "check-id")))
    (is (thrown? IllegalStateException (check :name "check-name")))
    (is (thrown? IllegalStateException (check :id "check-id" :name "check-name")))
    (is (thrown? IllegalStateException (check :id "check-id" :name "check-name" :tcp "localhost:1234")))
    (let [ch (check :id "check-id" :name "check-name" :notes "notes" :output "output"
                    :interval "1m" :tcp "localhost:1234" :service-id "service-id" :service-tags ["tag1"])]
      (is (instance? Check ch))
      (is (= (.getId ch) "check-id"))
      (is (= (.getName ch) "check-name"))
      (is (= (.getNotes ch) (Optional/of "notes")))
      (is (= (.getOutput ch) (Optional/of "output")))
      (is (= (.getInterval ch) (Optional/of "1m")))
      (is (= (.getTcp ch) (Optional/of "localhost:1234")))
      (is (= (.getServiceId ch) (Optional/of "service-id")))
      (is (= (.getServiceTags ch) ["tag1"])))))