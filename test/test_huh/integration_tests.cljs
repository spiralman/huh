(ns test.test-huh.integration-tests
  (:require [cljs.test :refer-macros [deftest is async]]
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

(defn stateful [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:value 1})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "stateful"}
               (dom/span #js {:className "state-value"}
                         (str (:value state)))
               (dom/button #js {:className "state-inc"
                                :type "button"
                                :onClick (fn [_]
                                           (om/set-state!
                                            owner
                                            :value
                                            (inc (:value state))))})))))


;; state changing
(deftest state-changes-on-click
  (async done
   (let [rendered-component (huh/rendered-component stateful
                                                    (huh/setup-state {}))]
     (huh/after-event
      :click #js {:target #js {}}
      (huh/in rendered-component "button")
      (fn [_]
        (is (= 2 (:value (huh/get-state rendered-component))))
        (done))))))
