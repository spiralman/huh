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

(defn -instrument [sub-component cursor m]
  {:sub-component sub-component
   :cursor cursor
   :m m}
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

(defn setup-state [value]
  (om/setup
   (if (satisfies? IAtom value)
     value
     (atom value))
   (gensym) nil)
  )

(defn rendered-component
  ([component state] (rendered-component component state nil))
  ([component state m]
     (binding [om/*state* state]
       (.render (om/build* component (om/to-cursor @state state []) m))
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

(defn -extract-m [tests]
  (if (map? (first tests))
    [(first tests) (rest tests)]
    [nil tests]
    )
  )

(defn rendered [component value & tests]
  (let [[m tests] (-extract-m tests)
        state (setup-state value)]
    (binding [om/*instrument* -instrument]
      (let [rendered-comp (rendered-component component state m)]
        (test-predicates tests rendered-comp {:in "rendered component"})
        )
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

(defn component-name [component]
  (.-name component)                    ; This isn't standard JS, but
                                        ; works enough places, for now
  )

(defn tag [expected-tag & tests]
  (fn -tag-pred [component]
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

(defn multiple-components? [component]
  (= (type component) js/Array))

(declare display-children)

(defn display-child
  ([component]
     (cond
      (not (nil? (.. component -props)))
      {
       :tag (tag-name component)
       :children (display-children (.. component -props -children))
       }
      (fn? component) {:sub-component (component-name component)}
      (string? component) {:text component}
      (satisfies? IDeref component) {:cursor @component}
      :else {:unknown "unknown"}
      )
     )
  )

(defn display-children [children]
  (if (multiple-components? children)
    (map #(display-child %) children)
    (display-child children)
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
             :expected test-count :actual child-count
             :actual-children (display-children children)}
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
           :expected test-count :actual 1
           :actual-children (display-children children)}
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
  (fn -text-pred [component]
    (if (= text component)
      true
      {:msg "Text does not match" :expected text :actual component}
      )
    )
  )

(defn sub-component
  ([expected-component cursor] (sub-component expected-component cursor nil))
  ([expected-component cursor m]
     (fn -sub-component-pred [component]
       (let [expected-name (component-name expected-component)
             actual-name (component-name (:sub-component component))
             actual-cursor @(:cursor component)
             actual-m (:m component)]
         (cond
          (not= expected-component (:sub-component component))
          {:msg "sub-component does not match"
           :expected expected-name :actual actual-name}

          (not= cursor actual-cursor)
          {:msg (str "sub-component cursor does not match for " expected-name)
           :expected cursor :actual actual-cursor}

          (and (not (nil? m)) (not= m actual-m))
          {:msg (str "sub-component m does not match for " expected-name)
           :expected m :actual actual-m}

          :else true
          )
         )
       )
     )
  )

(defn with-class [class-name]
  (fn -with-class-pred [component]
    (let [actual (.. component -props -className)]
      (if (= class-name actual)
        true
        {:msg "Class name does not match" :expected class-name :actual actual}
        )
      )
    )
  )

(defn with-attr [attr-name attr-value]
  (fn -with-attr-pred [component]
    (let [actual-value (aget (.. component -props) attr-name)]
      (if (= attr-value actual-value)
        true
        {:msg "Attribute value does not match"
         :expected attr-value :actual actual-value
         :attr attr-name :props (.. component -props)}
        )
      )
    )
  )

(defn after-event [event-attr event-arg component callback]
  ((or
    (aget (.. component -props) (name event-attr))
    (fn [_] (throw (js/Error. (str "No event handler for " event-attr))))
    )
   event-arg)
  (callback component)
  )
