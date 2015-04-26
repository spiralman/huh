(ns test.test-huh.core
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
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

;; To be deleted when there is reasonable test coverage for the
;; library.
(deftest test-coverage-is-good-enough
  (is (= 2 3)))

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
                               "test$test_huh$core$some_component"})}]
         (huh/rendered (div-component #js {}
                                      #(om/build some-component
                                                 {:cursor "value"}))
                       {}
                       (huh/containing (huh/tag "span")
                                       (huh/tag "span"))))))

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
             :expected "test$test_huh$core$some_component"
             :actual "test$test_huh$core$other_component"}))
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
           ({:msg "sub-component cursor does not match for test$test_huh$core$some_component"
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
           ({:msg "sub-component m does not match for test$test_huh$core$some_component"
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
