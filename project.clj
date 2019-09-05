(defproject com.github.csm/clojure-consul-client "0.2.0-SNAPSHOT"
  :description "Yet another consul client for Clojure"
  :url "https://github.com/csm/clojure-consul-client"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.github.csm/consul-api "0.1.1"]
                 [com.github.csm/vainglory "0.1.2"]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
