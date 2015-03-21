(ns test-huh.test.core
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [huh.core :as huh]))

;; To be deleted when there is reasonable test coverage for the
;; library.
(deftest test-coverage-is-good-enough
  (is (= 2 3)))
