(ns test-burgerboard.huh
  (:require-macros)
  (:require
   [cemerick.cljs.test :as t]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :refer [lower-case]]
   [goog.array :as g-array]
   )
  )


(defn do-report [& args]
  (apply t/do-report args)
  )

(defn -instrument [sub-component cursor m]
  (dom/div #js {:data-huh-sub-component sub-component
                :data-huh-cursor (.stringify js/JSON cursor)
                :data-huh-m (.stringify js/JSON m)})
  )

(defn decode-sub-component [sub-component]
  {:sub-component (js/eval (str "("
                                (.getAttribute sub-component
                                               "data-huh-sub-component")
                                ")"))
   :cursor (.parse js/JSON (.getAttribute sub-component "data-huh-cursor"))
   :m (.parse js/JSON (.getAttribute sub-component "data-huh-m"))}
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

(defn built-component
  ([component state] (built-component component state nil))
  ([component state m]
     (binding [om/*state* state]
       (om/build* component (om/to-cursor @state state []) m))
     )
  )

(defn rendered-component
  ([component state] (rendered-component component state nil))
  ([component state m]
     (let [built-c (built-component component state m)
           dom-el (js/document.createElement "div")]
       (.getDOMNode (js/React.renderComponent built-c dom-el))
     ))
  )

(defn get-child [component child]
  (aget (.-children component) child)
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
  (or
   (= (type component) js/Array)
   (sequential? component)
   )
  )

(declare display-children)

(defn display-child
  ([component]
     (cond
      (not (nil? (.-tagName component)))
      {
       :tag (.-tagName component)
       :children (display-children (g-array/toArray (.. component -children)))
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
    (let [children (g-array/toArray (.. component -children))
          test-count (count tests)]
      (let [child-count (.-length children)]
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
      )
    )
  )

(defn with-text [text]
  (fn -text-pred [component]
    (let [actual-text (.-textContent component)]
      (if (= text actual-text)
        true
        {:msg "Text does not match" :expected text :actual actual-text}
        )
      )
    )
  )

(defn sub-component
  ([expected-component cursor] (sub-component expected-component cursor nil))
  ([expected-component cursor m]
     (fn -sub-component-pred [component]
       (let [expected-name (component-name expected-component)
             {actual-sub-component :sub-component
              actual-cursor :cursor
              actual-m :m} (decode-sub-component component)
             actual-name (component-name actual-sub-component)]
         (cond
          (not= expected-component actual-sub-component)
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
    (let [actual (.. component -className)]
      (if (.contains (.. component -classList) class-name)
        true
        {:msg "Class name does not match" :expected class-name :actual actual}
        )
      )
    )
  )

(defn with-attr [attr-name attr-value]
  (fn -with-attr-pred [component]
    (let [actual-value (.getAttribute component attr-name)]
      (if (= attr-value actual-value)
        true
        {:msg "Attribute value does not match"
         :expected attr-value :actual actual-value
         :attr attr-name}
        )
      )
    )
  )

(defn after-event [event-attr event-arg component callback]
  ((aget js/React.addons.TestUtils.Simulate (name event-attr))
   component event-arg)
  (callback component)
  )
