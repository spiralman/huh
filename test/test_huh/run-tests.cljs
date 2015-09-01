(ns test.test-huh.run-tests
  (:require [cljs.test :refer-macros [run-all-tests] :as t]
            [test.test-huh.test-core]
            [test.test-huh.integration-tests]))

(defn ^:export run [cb]
  (defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
    (if (t/successful? m)
      (cb 0)
      (cb 1)))

  (enable-console-print!)
  (run-all-tests #"test.test-huh.*"))
