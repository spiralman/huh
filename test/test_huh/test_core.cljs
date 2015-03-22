(ns test.test-huh.core
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [huh.core :as huh]))

(defn rendered-div [props contents]
  (huh/rendered-component
   (fn [data owner]
     (reify
       om/IRender
       (render [this#]
         (dom/div props
                  contents))))
   (huh/setup-state {})))

;; To be deleted when there is reasonable test coverage for the
;; library.
(deftest test-coverage-is-good-enough
  (is (= 2 3)))

(deftest with-text-returns-true-when-text-present
  (is (= true ((huh/with-text "some text")
               (rendered-div #js {}
                             "some text")))))

(deftest with-text-returns-error-without-match
  (is (= {:msg "Text does not match" :expected "some text" :actual "other text"}
         ((huh/with-text "some text")
          (rendered-div #js {}
                        "other text")))))
