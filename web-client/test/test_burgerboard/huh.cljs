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

(defn rendered [component state & tests]
  (binding [om/*instrument* -instrument]
    (let [rendered-comp (.render (om/build* component state {}))]
      ;; Need to filter out true values, ans return true on an empty
      ;; list (same in tag), then apply that to all other sequences of
      ;; predicates
      (map (fn [test component] (test component)) tests (repeat rendered-comp))
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
          (map (fn [test c] (test c)) tests (repeat component))
          )
        )
      )
    )
  )

(defn containing [& tests]
  (fn [component]
    (let [children (js->clj (.. component -props -children))]
      (if (sequential? children)
        (and
         (= (count tests) (count children))
         (every? true?
                 (map (fn [pred child] (pred child))
                      tests children
                      )
                 )
         )
        (and
         (= 1 (count tests))
         ((nth tests 0) children) ;; Children is actually just one child
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

(defn sub-component [sub-component cursor]
  (fn [component]
    (= {:sub-component sub-component
        :cursor cursor}
       component
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
