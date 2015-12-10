(defproject logmon-data-generator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]

                 ;; this is for logging
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.13"] ;; need slf4j for metrics, so use slf4j and log4j (given in the Hadoop, YARN, Spark environment)

                 ;; metrics core
                 [metrics-clojure "2.6.0"]

                 ;; Riemann Metrics Reporter to be used with metrics core. Requires component, although not set as (transitive) dependency.
                 [metrics-clojure-riemann "2.6.0"]
                 [com.stuartsierra/component "0.3.1"]

                 ;; that's for my custom log4j appender
                 [gorillalabs/log4j-riemann-appender "0.1.0-SNAPSHOT"]

                 ])
