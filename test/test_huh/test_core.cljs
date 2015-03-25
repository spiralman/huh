(ns test.test-huh.core
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [huh.core :as huh]))

(defn rendered-div [props & contents]
  (huh/rendered-component
   (fn [data owner]
     (reify
       om/IRender
       (render [this#]
         (apply dom/div props
                contents))))
   (huh/setup-state {})))

;; To be deleted when there is reasonable test coverage for the
;; library.
(deftest test-coverage-is-good-enough
  (is (= 2 3)))

;; tag
(deftest tag-returns-true-when-tag-name-matches
  (is (= true ((huh/tag "div")
               (rendered-div #js {})))))

(deftest tag-returns-error-when-tag-name-doesnt-match
  (is (= {:msg "Tag does not match" :expected "span" :actual "div"}
         ((huh/tag "span") (rendered-div #js {})))))

(deftest tag-checks-sub-tests
  (is (= [{:in "tag div"}
          {:msg "Text does not match" :expected "text" :actual "other"}]
         ((huh/tag "div" (huh/with-text "text"))
          (rendered-div #js {} "other")))))

;; with-text
(deftest with-text-returns-true-when-text-present
  (is (= true ((huh/with-text "some text")
               (rendered-div #js {} "some text")))))

(deftest with-text-returns-error-without-match
  (is (= {:msg "Text does not match"
          :expected "some text" :actual "other text"}
         ((huh/with-text "some text")
          (rendered-div #js {} "other text")))))

;; with-class
(deftest with-class-returns-true-when-class-matches
  (is (= true ((huh/with-class "some-class")
               (rendered-div #js {:className "some-class"})))))

(deftest with-class-returns-true-when-class-present-in-list
  (is (= true ((huh/with-class "some-class")
               (rendered-div #js {:className "some-class other-class"})))))

(deftest with-class-returns-error-when-class-doesnt-match
  (is (= {:msg "Class name does not match"
          :expected "some-class" :actual "other-class"}
         ((huh/with-class "some-class")
          (rendered-div #js {:className "other-class"})))))

(deftest with-class-returns-error-when-class-not-specified
  (is (= {:msg "Class name does not match"
          :expected "some-class" :actual ""}
         ((huh/with-class "some-class") (rendered-div #js {})))))

;; with-attr
(deftest with-attr-returns-true-when-attr-present
  (is (= true ((huh/with-attr "type" "value")
               (rendered-div #js {:type "value"})))))

(deftest with-attr-returns-error-when-attr-not-specified
  (is (= {:msg "Attribute value does not match"
          :expected "value" :actual nil
          :attr "type"}
         ((huh/with-attr "type" "value")
          (rendered-div #js {})))))

(deftest with-attr-returns-error-when-attr-value-doesnt-match
  (is (= {:msg "Attribute value does not match"
          :expected "value" :actual "other"
          :attr "type"}
         ((huh/with-attr "type" "value")
          (rendered-div #js {:type "other"})))))

;; containing
(deftest containing-returns-true-if-child-assertions-and-count-match
  (is (= true ((huh/containing (huh/tag "div") (huh/tag "div"))
               (rendered-div #js {} (dom/div #js {}) (dom/div #js {}))))))

(deftest containing-returns-error-if-child-count-doesnt-match
  (is (= {:msg "Wrong number of child elements"
          :expected 2 :actual 1
          :actual-children '({:tag "div" :children ()})}
         ((huh/containing (huh/tag "div") (huh/tag "div"))
          (rendered-div #js {} (dom/div #js {}))))))

(defn stringify-comp [comp]
  (.stringify js/JSON comp))

(deftest containing-returns-errors-if-child-predicate-fails
  (is (= '({:msg "Tag does not match"
            :expected "span" :actual "div"}
           {:msg "Tag does not match"
            :expected "span" :actual "div"})
         ((huh/containing (huh/tag "span") (huh/tag "span") (huh/tag "span"))
          (rendered-div #js {}
                        (dom/div #js {})
                        (dom/span #js {})
                        (dom/div #js {}))))))
