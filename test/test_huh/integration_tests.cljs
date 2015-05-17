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

(defn inner [data owner {:keys [outer-owner]}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "inner"}))))

(defn outer [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "outer"}
               (dom/h2 #js {:className "heading"})
               (if (= 1 2)
                 (dom/div "wrong!"))
               (om/build inner {}
                         {:opts {:outer-owner owner}})))))

;; with-rendered
(deftest with-rendered-binds-name-to-rendered-component
  (is (rendered
       outer {}
       (huh/with-rendered [comp]
         (huh/tag "div"
                  (huh/with-class "outer")
                  (huh/containing
                   (huh/tag "h2"
                            (huh/with-class "heading"))
                   (huh/sub-component inner {}
                                      {:opts {:outer-owner
                                              (.-_owner (huh/get-rendered comp))}})))))))
