(ns test.test-huh.test-core
  (:require-macros [huh.core :refer [with-rendered]])
  (:require [cljs.test :refer-macros [deftest is run-tests async] :as t]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [huh.core :as huh]))

(defn el-component [el props & contents]
  (fn [data owner]
     (reify
       om/IRender
       (render [this#]
         (apply el props (map #(if (fn? %) (%)
                                   %)
                              contents))))))

(defn div-component [props & contents]
  (apply el-component dom/div props contents))

(defn some-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {}))))

(defn other-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {}))))

;; tag
(deftest tag-returns-true-when-tag-name-matches
  (is (= true (huh/rendered
               (div-component #js {}) {}
               (huh/tag "div")))))

(deftest tag-returns-error-when-tag-name-doesnt-match
  (is (= [{:in "rendered component"}
          {:msg "Tag does not match" :expected "span" :actual "div"}]
         (huh/rendered (div-component #js {}) {} (huh/tag "span")))))

(deftest tag-checks-sub-tests
  (is (= [{:in "rendered component"}
          [{:in "tag div"}
           {:msg "Text does not match" :expected "text" :actual "other"}]]
         (huh/rendered (div-component #js {} "other") {}
                       (huh/tag "div" (huh/with-text "text"))))))

;; with-text
(deftest with-text-returns-true-when-text-present
  (is (= true (huh/rendered (div-component #js {} "some text") {}
                            (huh/with-text "some text")))))

(deftest with-text-returns-error-without-match
  (is (= [{:in "rendered component"}
          {:msg "Text does not match"
          :expected "some text" :actual "other text"}]
         (huh/rendered (div-component #js {} "other text") {}
                       (huh/with-text "some text")))))

;; with-class
(deftest with-class-returns-true-when-class-matches
  (is (= true (huh/rendered (div-component #js {:className "some-class"}) {}
                            (huh/with-class "some-class")))))

(deftest with-class-returns-true-when-class-present-in-list
  (is (= true (huh/rendered
               (div-component #js {:className "some-class other-class"}) {}
               (huh/with-class "some-class")))))

(deftest with-class-returns-error-when-class-doesnt-match
  (is (= [{:in "rendered component"}
          {:msg "Class name does not match"
           :expected "some-class" :actual "other-class"}]
         (huh/rendered (div-component #js {:className "other-class"}) {}
                       (huh/with-class "some-class")))))

(deftest with-class-returns-error-when-class-not-specified
  (is (= [{:in "rendered component"}
          {:msg "Class name does not match"
           :expected "some-class" :actual ""}]
         (huh/rendered (div-component #js {}) {}
                       (huh/with-class "some-class")))))

;; with-attr
(deftest with-attr-returns-true-when-attr-present
  (is (= true (huh/rendered (div-component #js {:type "value"}) {}
                            (huh/with-attr "type" "value")))))

(deftest with-attr-returns-error-when-attr-not-specified
  (is (= [{:in "rendered component"}
          {:msg "Attribute value does not match"
           :expected "value" :actual nil
           :attr "type"}]
         (huh/rendered (div-component #js {}) {}
                       (huh/with-attr "type" "value")))))

(deftest with-attr-returns-error-when-attr-value-doesnt-match
  (is (= [{:in "rendered component"}
          {:msg "Attribute value does not match"
           :expected "value" :actual "other"
           :attr "type"}]
         (huh/rendered (div-component #js {:type "other"}) {}
                       (huh/with-attr "type" "value")))))

;; with-prop
(deftest with-prop-returns-true-if-prop-value-matches
  (is (= true
         (huh/rendered (el-component dom/input #js {:value "some-value"
                                                    :onChange identity})
                       {}
                       (huh/with-prop "value" "some-value")))))

(deftest with-prop-returns-error-if-prop-value-doesnt-matches
  (is (= [{:in "rendered component"}
          {:msg "Wrong value for prop 'value'"
           :expected "some-value" :actual "other-value"}]
         (huh/rendered (el-component dom/input #js {:value "other-value"
                                                    :onChange identity})
                       {}
                       (huh/with-prop "value" "some-value")))))

;; containing
(deftest containing-returns-true-if-child-assertions-and-count-match
  (is (= true (huh/rendered
               (div-component #js {} (dom/div #js {}) (dom/div #js {}))
               {}
               (huh/containing (huh/tag "div") (huh/tag "div"))))))

(deftest containing-returns-error-if-child-count-doesnt-match
  (is (= [{:in "rendered component"}
          {:msg "Wrong number of child elements"
           :expected 2 :actual 1
           :actual-children '({:tag "div" :children ()})}]
         (huh/rendered (div-component #js {} (dom/div #js {})) {}
                       (huh/containing (huh/tag "div") (huh/tag "div"))))))

(deftest containing-returns-errors-if-child-predicate-fails
  (is (= [{:in "rendered component"}
          '({:msg "Tag does not match"
             :expected "span" :actual "div"}
            {:msg "Tag does not match"
             :expected "span" :actual "div"})]
         (huh/rendered (div-component #js {}
                                      (dom/div #js {})
                                      (dom/span #js {})
                                      (dom/div #js {}))
                       {}
                       (huh/containing (huh/tag "span")
                                       (huh/tag "span")
                                       (huh/tag "span"))))))

(deftest containing-handles-extra-predicates-in-sub-tags
  (is (= '({:in "rendered component"}
           (({:in "tag div"}
             {:msg "Class name does not match"
              :expected "div-class" :actual "other-class"})))
         (huh/rendered (div-component #js {}
                                      (dom/div #js {:className "other-class"}))
                       {}
                       (huh/containing
                        (huh/tag "div"
                                 (huh/with-class "div-class")))))))

(deftest containing-handles-multiple-nesting
  (is (= '({:in "rendered component"}
           (({:in "tag div"}
             (({:in "tag span"}
               {:msg "Class name does not match"
                :expected "span-class" :actual "other-class"})))))
         (huh/rendered
          (div-component #js {}
                         (dom/div #js {:className "div-class"}
                                  (dom/span #js {:className "other-class"})))
          {}
          (huh/containing
           (huh/tag "div"
                    (huh/with-class "div-class")
                    (huh/containing
                     (huh/tag "span"
                              (huh/with-class "span-class")))))))))

(deftest containing-handles-nested-with-prop
  (is (= '({:in "rendered component"}
           (({:in "tag input"}
             {:msg "Wrong value for prop 'value'"
              :expected "some-value" :actual "other-value"})))
         (huh/rendered
          (div-component #js {}
                         (dom/input #js {:value "other-value"
                                         :onChange identity}))
          {}
          (huh/containing
           (huh/tag "input"
                    (huh/with-prop "value" "some-value")))))))

(deftest containing-handles-interleaved-text
  (is (= '({:in "rendered component"}
           (({:in "tag div"}
             (({:in "tag span"}
               {:msg "Class name does not match"
                :expected "span-class" :actual "other-class"})))))
         (huh/rendered
          (div-component #js {}
                         (dom/div #js {:className "div-class"}
                                  "some text"
                                  (dom/span #js {:className "other-class"})
                                  "other text"))
          {}
          (huh/containing
           (huh/tag "div"
                    (huh/with-class "div-class")
                    (huh/containing
                     (huh/tag "span"
                              (huh/with-class "span-class")))))))))

(deftest containing-handles-textual-nodes
  (is (= '({:in "rendered component"}
           (({:in "tag div"}
             (({:in "tag span"}
               {:msg "Text does not match"
                :expected "some text" :actual "other text"})))))
         (huh/rendered
          (div-component #js {}
                         (dom/div #js {}
                                  (dom/span #js {} "other text")))
          {}
          (huh/containing
           (huh/tag "div"
                    (huh/containing
                     (huh/tag "span"
                              (huh/with-text "some text")))))))))

(deftest containing-prints-component-name-when-count-mismatched
  (is (= [{:in "rendered component"}
          {:msg "Wrong number of child elements"
           :expected 2 :actual 1
           :actual-children '({:sub-component
                               "test$test_huh$test_core$some_component"})}]
         (huh/rendered (div-component #js {}
                                      #(om/build some-component
                                                 {:cursor "value"}))
                       {}
                       (huh/containing (huh/tag "span")
                                       (huh/tag "span"))))))

(deftest containing-ignores-nil-children
  (is (= true (huh/rendered
               (div-component #js {}
                              (dom/div #js {})
                              (if (= 2 3)
                                (dom/div "won't exist"))
                              #(om/build some-component {}))
               {}
               (huh/containing
                (huh/tag "div")
                (huh/sub-component some-component {}))))))

(deftest containing-handles-text-when-child-count-does-not-match
  (is (= [{:in "rendered component"}
          {:msg "Wrong number of child elements"
           :expected 2 :actual 1
           :actual-children '({:tag "div" :children ()})}]
         (huh/rendered (div-component #js {}
                                      (dom/div #js {} "some text")
                                      "more text")
                       {}
                       (huh/containing (huh/tag "div") (huh/tag "div"))))))

;; sub-component
(deftest sub-component-returns-true-if-sub-component-and-cursor-and-m-match
  (is (= true
         (huh/rendered
          (div-component #js {}
                         #(om/build some-component
                                    {:cursor "value"}
                                    {:opts {:some-opt 1}}))
          {}
          (huh/containing
           (huh/sub-component some-component
                              {:cursor "value"}
                              {:opts {:some-opt 1}}))))))

(deftest sub-component-returns-true-if-sub-component-and-cursor-match-ignoring-m
  (is (= true
         (huh/rendered
          (div-component #js {}
                         #(om/build some-component
                                    {:cursor "value"}
                                    {:opts {:some-opt 1}}))
          {}
          (huh/containing
           (huh/sub-component some-component
                              {:cursor "value"}))))))

(deftest sub-component-returns-error-when-component-does-not-match
  (is (= '({:in "rendered component"}
           ({:msg "sub-component does not match"
             :expected "test$test_huh$test_core$some_component"
             :actual "test$test_huh$test_core$other_component"}))
         (huh/rendered
          (div-component #js {}
                         #(om/build other-component
                                    {:cursor "value"}
                                    {:opts {:some-opt 1}}))
          {}
          (huh/containing
           (huh/sub-component some-component
                              {:cursor "value"}))))))

(deftest sub-component-returns-error-when-cursor-does-not-match
  (is (= '({:in "rendered component"}
           ({:msg "sub-component cursor does not match for test$test_huh$test_core$some_component"
             :expected {:cursor "value"}
             :actual {:cursor "other value"}}))
         (huh/rendered
          (div-component #js {}
                         #(om/build some-component
                                    {:cursor "other value"}
                                    {:opts {:some-opt 1}}))
          {}
          (huh/containing
           (huh/sub-component some-component
                              {:cursor "value"}))))))

(deftest sub-component-returns-error-when-m-does-not-match
  (is (= '({:in "rendered component"}
           ({:msg "sub-component m does not match for test$test_huh$test_core$some_component"
             :expected {:opts {:some-opt 1}}
             :actual {:opts {:other-opt 2}}}))
         (huh/rendered
          (div-component #js {}
                         #(om/build some-component
                                    {:cursor "value"}
                                    {:opts {:other-opt 2}}))
          {}
          (huh/containing
           (huh/sub-component some-component
                              {:cursor "value"}
                              {:opts {:some-opt 1}}))))))

;; in
(deftest in-returns-dom-node-without-selector
  (let [state (huh/setup-state {})
        selected (huh/in (huh/rendered-component
                          (div-component #js {:id "root-component"}) state))]
    (is (= "DIV" (.-tagName selected)))
    (is (= "root-component" (.-id selected)))))

(deftest in-returns-dom-node-with-empty-selector
  (let [state (huh/setup-state {})
        selected (huh/in (huh/rendered-component
                          (div-component #js {:id "root-component"}) state)
                         "")]
    (is (= "DIV" (.-tagName selected)))
    (is (= "root-component" (.-id selected)))))

(deftest in-returns-inner-dom-node-with-selector
  (let [state (huh/setup-state {})
        selected (huh/in (huh/rendered-component
                          (div-component #js {:id "root-component"}
                                         (dom/div #js {:id "child"}
                                                  (dom/span
                                                   #js {:id "grandchild"})))
                          state)
                         "#grandchild")]
    (is (= "SPAN" (.-tagName selected)))
    (is (= "grandchild" (.-id selected)))))

(deftest after-event-triggers-event-and-calls-callback-asynchronously
  (async
   done
   (let [event-args (atom nil)
         state (huh/setup-state {})]
     (huh/after-event
      :click #js {:event "args"}
      (huh/in (huh/rendered-component
               (div-component #js {:onClick #(reset! event-args %)}) state))
      (fn [clicked-node]
        (is (= "DIV" (.-tagName clicked-node)))
        (is (= "args" (aget @event-args "event")))
        (done))))))

;; with-rendered
(deftest with-rendered-executes-assertion-and-provides-parameter
  (is (= true
         ((with-rendered [arg]
            #(= arg :input))
          :input))))

(defn stateful-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:value 1})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {}))))

(deftest get-state-returns-state-of-rendered-component
  (let [rendered (huh/rendered-component stateful-component
                                         (huh/setup-state {}))]
    (is (= {:value 1} (huh/get-state rendered)))))

(deftest get-state-returns-keys-of-state-of-rendered-component
  (let [rendered (huh/rendered-component stateful-component
                                         (huh/setup-state {}))]
    (is (= 1 (huh/get-state rendered :value)))))
