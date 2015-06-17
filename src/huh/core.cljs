(ns huh.core
  (:require-macros)
  (:require
   [cemerick.cljs.test :as t]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :refer [lower-case]]
   [goog.array :as g-array]
   cljsjs.react
   )
  )

(defprotocol IRendered
  "Interface used to define all rendered components or elements. It is
returned by `rendered-component, and passed to all the test predicate
functions."
  (get-node [c] "Returns the DOM node corresponding to the Component or Element")
  (get-rendered [c] "Returns the Component or Element that was rendered")
  (get-props [c] "Returns the props for the rendered component")
  (get-component [c] "Returns the Om component, or nil"))

(defn get-state
  "Accesses the state of a rendered Om component. If `korks` is
specified, it returns the value in the state at that key, as by
`get-in`. May only be called on a component returned by
`rendered-component`."
  ([rendered] (om/get-state (get-component rendered)))
  ([rendered korks] (om/get-state (get-component rendered) korks)))

(defn ^:no-doc do-report [& args]
  (apply t/do-report args)
  )

(defn ^:no-doc component-name [component]
  ;; component is a function; this isn't really standardized JS, but
  ;; it works in most browsers anyway.
  (.-name component))

(defn ^:no-doc -instrument [sub-component cursor m]
  (dom/div #js {:className "huh-sub-component"
                :data-sub-component sub-component
                :data-cursor cursor
                :data-m m}))

(defn ^:no-doc decode-sub-component [sub-component]
  (let [props (get-props sub-component)
        cursor (aget props "data-cursor")]
    {
     :sub-component (aget props "data-sub-component")
     :cursor (if (satisfies? IDeref cursor)
               @cursor
               cursor
               )
     :m (aget props "data-m")
     }
    )
  )

(defn ^:no-doc sub-component? [node]
  (.contains (.. node -classList) "huh-sub-component"))

(defn ^:no-doc rendered-sub-component [dom-node component]
  (reify IRendered
    (get-node [_] dom-node)
    (get-rendered [_] component)
    (get-props [_] (.. component -props))
    (get-component [_] nil)))

(defn ^:no-doc test-predicates
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

(defn setup-state
  "Builds an Om state object from a raw ClojureScript data structure,
  or an atom containing a data structure. State passed to
  `rendered-component` should be built using this function."
  [value]
  (om/setup
   (if (satisfies? IAtom value)
     value
     (atom value))
   (gensym) nil)
  )

(defn ^:no-doc built-component
  ([component state] (built-component component state nil))
  ([component state m]
     (binding [om/*state* state]
       (om/build* component (om/to-cursor @state state []) m))
     )
  )

(defn rendered-component
  "Renders a component into the DOM and returns it, but does not run
assertions against it. Useful for testing stateful components."
  ([component state] (rendered-component component state nil))
  ([component state m]
     (let [built-c (built-component component state m)
           dom-el (js/document.createElement "div")
           component (js/React.addons.TestUtils.renderIntoDocument built-c)]
       (reify
         IRendered
         (get-node [_] (om/get-node component))
         (get-rendered [_] (.-_renderedComponent component))
         (get-props [_] (.. component -_renderedComponent -props))
         (get-component [_] component))
       ))
  )

(defn ^:no-doc rendered-element [dom-node react-element]
  (if (sub-component? dom-node)
    (rendered-sub-component dom-node react-element)
    (reify
      IRendered
      (get-node [_] dom-node)
      (get-rendered [_] react-element)
      (get-props [_] (.-props react-element))
      (get-component [_] nil))))

(defn ^:no-doc -extract-m [tests]
  (if (map? (first tests))
    [(first tests) (rest tests)]
    [nil tests]
    )
  )

(defn rendered
  "Renders a component using the provided data (and an optional `m`
  value) and then runs assertions against that component.

Normally, this is used inside the `is` macro in `clojurescript.test`:

    (is (rendered component {:state \"value\"}
                  (tag \"DIV\")))"
  [component
  value & tests]
  (let [[m tests] (-extract-m tests)
        state (setup-state value)]
    (binding [om/*instrument* -instrument]
      (let [rendered-comp (rendered-component component state m)]
        (test-predicates tests rendered-comp {:in "rendered component"})
        ))
    )
  )

(defn ^:no-doc child-nodes [component]
  (->
   (get-node component)
   (.-children)
   (js/Array.prototype.slice.call)
   (js->clj)))

(defn ^:no-doc text? [rendered]
  (string? (get-rendered rendered)))

(defn ^:no-doc children-of [component]
  (let [children #js []]
    (.forEach js/React.Children (.-children (get-props component))
              #(.push children %))
    (remove text? (map rendered-element
                       (child-nodes component) (remove nil? (seq children))))))

(defn in
  "Returns the first DOM element within a given component that matches the selector.

If the selector is empty or not provided, it returns the root DOM node
of the component."
  ([component] (get-node component))
  ([component selector]
     (let [component-node (get-node component)]
       (if (= selector "")
         component-node
         (.querySelector component-node selector)
         )
       )
     )
  )

(defn ^:no-doc tag-name [component]
  (->
   (get-node component)
   (.-tagName)
   (lower-case)))

(defn tag
  "Asserts that a component or element is rendered as a particular DOM
  element, by tag, and runs an additional assertions against that
  component. Tag names are case insensitive."
  [expected-tag & tests]
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

(declare display-children)

(defn ^:no-doc display-child
  ([component]
     (if (sub-component? (get-node component))
       {:sub-component (->
                        (decode-sub-component component)
                        (:sub-component)
                        (component-name))}
       {:tag (tag-name component)
        :children (display-children (children-of component))})))

(defn ^:no-doc display-children [children]
  (map #(display-child %) children))

(defn containing
  "Asserts that the component contains the given children. The number
of predicates must exactly match the number of children, or the
assertion will fail.

Normally, each predicate will be a `tag`, containing optional
additional tests against that child element:

    (containing
     (tag \"DIV\"
          (with-class \"first-child\"))
     (tag \"SPAN\"
          (with-class \"second-child\")))"
  [& tests]
  (fn -containing-pred [component]
    (let [children (children-of component)
          test-count (count tests)
          child-count (count children)]
      (if (not= test-count child-count)
        {:msg "Wrong number of child elements"
         :expected test-count :actual child-count
         :actual-children (display-children children)
         }
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

(defn with-prop
  "Asserts that a component has a prop with a particular value. This
  is useful for asserting things about \"bound\" attributes, such as
  the `value` of an input element."
  [prop-name expected-value]
  (fn -with-prop-pred [component]
    (let [actual-value (aget (get-props component) prop-name)]
      (if (not= expected-value actual-value)
        {:msg (str "Wrong value for prop '" prop-name "'")
         :expected expected-value :actual actual-value}
        true
        )
      ))
  )

(defn with-text
  "Asserts that an element has the given text. Uses the `textContent`
  property of the DOM node to gather the text of all child elements"
  [text]
  (fn -text-pred [component]
    (let [component (get-node component)
          actual-text (.-textContent component)]
      (if (= text actual-text)
        true
        {:msg "Text does not match" :expected text :actual actual-text}
        )
      )
    )
  )

(defn sub-component
  "Asserts that another Om component was rendered within the component
being tested.

When rendering with `rendered` or `rendered-component`, Huh prevents
sub-components (as built via `om/build` or `om/build-all`) from
actually being built. Instead, this predicate may be used to assert
that the correct sub-component was built, using the correct cursor and
options.

`expected-component` should be the function defining the component,
and `cursor` should be a raw ClojureScript data structure describing
the *contents* of the cursor passed to the sub-component"
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

(defn with-class
  "Asserts that the DOM node was given a particular CSS class.

Uses the `classList` property, so can be specified multiple times to
  test for multiple classes."
  [class-name]
  (fn -with-class-pred [component]
    (let [node (get-node component)
          actual (.. node -className)]
      (if (.contains (.. node -classList) class-name)
        true
        {:msg "Class name does not match" :expected class-name :actual actual}
        )
      )
    )
  )

(defn with-attr
  "Asserts that a DOM element has a given attribute with a given value"
  [attr-name attr-value]
  (fn -with-attr-pred [component]
    (let [component (get-node component)
          actual-value (.getAttribute component attr-name)]
      (if (= attr-value actual-value)
        true
        {:msg "Attribute value does not match"
         :expected attr-value :actual actual-value
         :attr attr-name}
        )
      )
    )
  )

(defn after-event
  "Simulates an event against a DOM element, and then invokes the
callback function, to test that stateful components handle events
correctly.

This is generally combined with `rendered-component` and `in` to
select the DOM element, and the callback function will execute
assertions to test that the component handled the event correctly."
  [event-attr event-arg dom-node callback]
  ((aget js/React.addons.TestUtils.Simulate (name event-attr))
   dom-node event-arg)
  (callback dom-node)
  )
