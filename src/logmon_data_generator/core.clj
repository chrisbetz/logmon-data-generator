(ns logmon-data-generator.core
  (:require [clojure.tools.logging :as log]
            [metrics.core :refer [new-registry]]
            [metrics.counters :refer [counter inc!]]
            [metrics.reporters.console :as console]
            [metrics.reporters.riemann :as riemann]))

(def reg (new-registry))
(def sample-counter (counter reg ["logmon-data-generator sample"]))
(def CR (console/reporter reg {}))
(def RR (riemann/reporter (riemann/make-riemann "localhost" 5555) reg {}))

(defn start
  "Generates logs and metrics to check you logmon system"
  []
  (console/start CR 2)
  (riemann/start RR 2))

(defn log! []
  (log/info "This is just a random info message.")
  (log/warn (Exception. "A random exception") "This is just a random warn message.")
  )

(defn count! []
  (inc! sample-counter))

(defn stop []
  (console/stop CR)
  (riemann/stop RR)
  )
