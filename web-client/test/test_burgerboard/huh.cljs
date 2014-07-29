(ns test-burgerboard.huh
  (:require-macros)
  (:require
   [cemerick.cljs.test :as t]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :refer [upper-case]]
   )
  )


(defn do-report [& args]
  (apply t/do-report args)
  )

(defn -instrument [sub-component cursor _]
  {:sub-component sub-component
   :cursor cursor
   }
  )

(defn apply-all
  ;; calls each function in fns with arg and returns a seq of the
  ;; results (like the inverse of map)
  [fns arg]
  (map (fn [f a] (f a)) fns (repeat arg))
  )

(defn test-predicates
  ;; Checks all predicates against a component, returns true if they
  ;; all pass, or a seq of failures if any failed.
  [preds component msg]
  (if-let [failures
             (seq (filter
                   (fn [result] (not= true result))
                   (apply-all preds component)))]
    (conj failures msg)
    true
    )
  )

(defn rendered [component state & tests]
  (binding [om/*instrument* -instrument]
    (let [rendered-comp (.render (om/build* component state {}))]
      (test-predicates tests rendered-comp {:in "rendered component"})
      )
    )
  )

(defn tag [tag-name & tests]
  (fn [component]
    (let [actual (.-tagName component)]
      (if-not (= (upper-case tag-name) actual)
        {:msg "Tag does not match" :expected tag-name :actual actual}
        (if (empty? tests)
          true
          (test-predicates tests component {:in (str "tag " tag-name)})
          )
        )
      )
    )
  )

(defn containing [& tests]
  (fn [component]
    (let [children (js->clj (.. component -props -children))
          test-count (count tests)]
      (if (sequential? children)
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
  (fn [component]
    (let [expected-name (component-name sub-component)
          actual-name (component-name (:sub-component component))]
      (cond
       (not= sub-component (:sub-component component))
       {:msg "sub-component does not match"
        :expected expected-name :actual actual-name}

       (not= cursor (:cursor component))
       {:msg (str "sub-component cursor does not match for " expected-name)
        :expected cursor :actual (:cursor component)}

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
