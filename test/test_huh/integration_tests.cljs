(ns test.test-huh.integration-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [huh.core :as huh :refer [rendered] :include-macros true]))

(defn single [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "single"}
               (dom/span #js {:className "first"} "First")
               (dom/span #js {:className "second"} "Second")))))

(deftest single-component-rendered-correctly
  (is (rendered
       single {} {}
       (huh/tag "div"
                (huh/with-class "single")
                (huh/containing
                 (huh/tag "span"
                          (huh/with-class "first")
                          (huh/with-text "First"))
                 (huh/tag "span"
                          (huh/with-class "second")
                          (huh/with-text "Second")))))))
