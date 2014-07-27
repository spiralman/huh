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
      ((apply every-pred tests) rendered-comp)
      )
    )
  )

(defn tag [tag-name & tests]
  (fn [component]
    (and
     (= (upper-case tag-name) (.-tagName component))
     (if (empty? tests)
       true
       ((apply every-pred tests) component)
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
    (= text component)
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
    (= class-name (.. component -props -className))
    )
  )
