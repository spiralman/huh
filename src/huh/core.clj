(ns huh.core
  (:require [cemerick.cljs.test :as t]
            )
  )

(defmethod t/assert-expr 'rendered [msg form]
  `(let [result# ~form]
     (do-report (t/test-context)
                {:type (if (= true result#) :pass :fail), :message ~msg,
                 :expected '~form, :actual result#}
                )
     )
  )

(defmacro with-rendered
  "Binds the values specified via `params`, and then runs the
assertion, passing the first parameter.

This can be used within `rendered` to gain access to the rendered
component, if it needs to be accessed inside the assertions."
  [params assertion]
  `(fn ~params
     (~assertion ~(nth params 0))
     )
  )
