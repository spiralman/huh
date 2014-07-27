(ns test-burgerboard.huh
  (:require [cemerick.cljs.test :as t]
            )
  )

(defmethod t/assert-expr 'rendered [msg form]
  `(let [~'-failed-predicates []]
     (if ~form
       (do-report (t/test-context)
                  {:type :pass, :message ~msg,
                   :expected '~form, :actual ~form}
                  )
       (do-report (t/test-context)
                  {:type :fail, :message ~msg,
                   :expected '~form, :actual ~'-failed-predicates}
                  )
       )
     )
  )
