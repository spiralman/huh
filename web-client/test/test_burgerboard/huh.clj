(ns test-burgerboard.huh
  (:require [cemerick.cljs.test :as t]
            )
  )

(defmethod t/assert-expr 'rendered [msg form]
  `(let [result# ~form]
     (println result)
     (do-report (t/test-context)
                {:type (if (= true result#) :pass :fail), :message ~msg,
                 :expected '~form, :actual result#}
                )
     )
  )
