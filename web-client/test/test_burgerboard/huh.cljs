(ns test-burgerboard.huh
  (:require-macros)
  (:require
   [cemerick.cljs.test :as t]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :refer [lower-case]]
   )
  )


(defn do-report [& args]
  (apply t/do-report args)
  )

(defn -instrument [sub-component cursor _]
  {:sub-component sub-component
   :cursor cursor}
  )

(defn test-predicates
  ;; Checks all predicates against a component, returns true if they
  ;; all pass, or a seq of failures if any failed.
  [preds component msg]
  (if-let [failures
             (seq (filter
                   (fn [result] (not= true result))
                   ((apply juxt preds) component)))]
    (conj failures msg)
    true
    )
  )

(defn rendered-component [component value]
  (let [state (om/setup
               (if (satisfies? IAtom value)
                 value
                 (atom value))
               (gensym) nil)]
    (binding [om/*state* state]
      (.render (om/build* component (om/to-cursor @state state [])))
      )
    )
  )

(defn get-child [component child]
  (let [children (js->clj (.. component -props -children))]
    (if (sequential? children)
      (nth children child)
      children
      )
    )
  )

(defn in [component & accessors]
  (reduce get-child component accessors)
  )

(defn rendered [component state & tests]
  (binding [om/*instrument* -instrument]
    (let [rendered-comp (rendered-component component state)]
      (test-predicates tests rendered-comp {:in "rendered component"})
      )
    )
  )

(defn tag-name [component]
  (cond
   (fn? (.-getDisplayName component)) (.getDisplayName component)
   (not (nil? (.-tagName component))) (lower-case (.-tagName component))
   :else (.-type (.-props component))
    )
  )

(defn tag [expected-tag & tests]
  (fn [component]
    (let [actual (tag-name component)]
      (if-not (= expected-tag actual)
        {:msg "Tag does not match" :expected expected-tag :actual actual}
        (if (empty? tests)
          true
          (test-predicates tests component {:in (str "tag " expected-tag)})
          )
        )
      )
    )
  )

(defn containing [& tests]
  (fn -containing-pred [component]
    (let [children (.. component -props -children)
          test-count (count tests)]
      (if (= (type children) js/Array)
        ;; React annoyingly makes children = the single child element,
        ;; when there is only one, instead of a list of one
        (let [child-count (count children)]
          (if (not= test-count child-count)
            {:msg "Wrong number of child elements"
             :expected test-count :actual child-count}
            (if-let [failures
                     (seq (filter (fn [result] (not= true result))
                                  (map (fn [pred child] (pred child))
                                       tests children
                                       )))]
              failures
              true
              )
            )
          )
        (if (not= 1 test-count) ;; Children is actually just one child
          {:msg "Wrong number of child elements"
           :expected test-count :actual 1}
          ((nth tests 0) children)
          )
        )
      )
    )
  )

(defn nothing [component]
  (if (nil? component)
    true
    {:msg "non empty container" :expected nil :actual component}
    )
  )

(defn text [text]
  (fn [component]
    (if (= text component)
      true
      {:msg "Text does not match" :expected text :actual component}
      )
    )
  )

(defn component-name [component]
  (.-name component)                    ; This isn't standard JS, but
                                        ; works enough places, for now
  )

(defn sub-component [sub-component cursor]
  (fn -sub-component-pred [component]
    (let [expected-name (component-name sub-component)
          actual-name (component-name (:sub-component component))
          actual-component @(:cursor component)]
      (cond
       (not= sub-component (:sub-component component))
       {:msg "sub-component does not match"
        :expected expected-name :actual actual-name}

       (not= cursor actual-component)
       {:msg (str "sub-component cursor does not match for " expected-name)
        :expected cursor :actual actual-component}

       :else true
       )
      )
    )
  )

(defn with-class [class-name]
  (fn [component]
    (let [actual (.. component -props -className)]
      (if (= class-name actual)
        true
        {:msg "Class name does not match" :expected class-name :actual actual}
        )
      )
    )
  )

(defn with-attr [attr-name attr-value]
  (fn [component]
    (let [actual-value (aget (.. component -props) attr-name)]
      (if (= attr-value actual-value)
        true
        {:msg "Attribute value does not match"
         :expected attr-value :actual actual-value}
        )
      )
    )
  )

(defn after-event [event-attr event-arg component callback]
  (do
    ((aget (.. component -props) (name event-attr)) event-arg)
    (callback component)
    )
  )
