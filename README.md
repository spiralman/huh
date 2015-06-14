# huh

[![Circle CI](https://circleci.com/gh/spiralman/huh.svg?style=svg)](https://circleci.com/gh/spiralman/huh)

Huh is a library for writing Unit Tests against Om applications.

It provides some convenience functions for managing the component
life-cycle and state, and a set of assertions to be run against the
React elements returned by components' `render` methods.

## Getting Started ##

Huh is hosted on Clojars. The latest version is:

[![Clojars Project](http://clojars.org/huh/latest-version.svg)](http://clojars.org/huh)

Using Leiningen, define a `project.clj`:

```clojure
(defproject om-app "0.1.0"
  :description "An application written in ClojureScript with Om"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]
                 [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.12.2-4"]
                 [huh "0.9.3"]
  :plugins [[lein-cljsbuild "1.0.5"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :cljsbuild {
              :builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "out/huh-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit" ["phantomjs" :runner
                                      "js-libs/es5-shim.js"
                                      "out/huh-test.js"]}})
```

Note two dependencies:

1. You must use `react-with-addons` for your unit tests. If you want
   just `react` in your application, you can use a separate profile
   for writing unit tests.
1. If you want to run your unit tests in PhantomJS, you will need
es5-shim (or a similar library), because React depends on it.

Assuming you're using `clojurescript.test`, a test for a component
looks like:

```clojure
(ns test.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var
                            done)])
  (:require [cemerick.cljs.test :as t]
            [huh.core :as huh :refer [rendered] :include-macros true]))

(deftest component-renders-controls
  (is (rendered
       my-component {:application "state"}
       (huh/tag "div"
                (huh/with-class "app-root")
                (huh/containing
                 (huh/tag "span"
                          (huh/with-text "Text in a span"))
                 (huh/sub-component inner-component
                                    {:inner "cursor"}))))))
```

For a more complete example of testing a medium-sized application, see
the "Burgerboard" application, which was the original impetus for the
library:
https://github.com/spiralman/burgerboard/tree/master/web-client/src/burgerboard_web

## Why the Name? ##

If "om" was the sound that created the universe, "huh" must certainly
have been the sound uttered upon seeing the result. Whether it is
pronounced "huh?" or "huh!" depends on whether your tests are
currently passing. :-)

## Testing Components ##

In `huh`, the `rendered` function takes a component, a data-structure
representing that component's application-state cursor, an optional
`m` value, and a series of predicates which will be each executed on
the rendered component.

`rendered` will return `true` if all predicates pass, otherwise it
will return a data-structure describing which sub-predicates
failed. An implementation of the multimethod backing the `is` macro is
provided, to fail and print the data-structure if any of the
predicates fail.

Each assertion function (`tag`, `with-class`, etc.) takes the expected
values for the matching component and returns a function predicate to
check that the expected values match the rendered DOM element.

### Matching Tags ###

The `tag` predicate matches the name of a tag with the rendered DOM
element. It also takes any number of sub-predicates which will all be
matched against the same DOM element.

### Matching Children ###

The `containing` predicate takes a sequence of predicates and matches
them, in order, with the children of the DOM element. It will fail if
the number of children doesn't match the number of predicates, so
there must be exactly one predicate for each child element. Generally,
this will be a `tag` predicate, containing more tests against that
child element.

Currently there is no "don't care" predicate, but that could be
implemented simply with a `(constantly true)`.

### Matching Sub-Components ###

In `huh`, each test is intended to only test a single component, so
any sub-components (built with `om/build` or `om/build-all`) will not
actually be built.

Instead, the test should assert that the appropriate sub-component was
built, with the appropriate data and options, using the
`sub-component` predicate. It takes a component function to match,
expected data, and an optional `m` value for the sub-component.

## Testing Event Handlers and Local State Changes ##

It is often necessary to test that components handle DOM events
correctly, possibly by inspecting their local state after an event, or
by asserting that they respond to the event in some other way.

`huh` provides the `rendered-component` function for this task. Rather
than validating predicates on the component, it simply returns the
rendered component.

The `after-event` function can then be used to trigger a DOM event on
an element in the component. It takes the event name, the arguments to
pass to the callback, the component to invoke the event on, and a
function to be called after the event has been dispatched.

The `in` function can be used to select a sub-element of the rendered
component on which to trigger the event. It takes the rendered
component and a query selector string, and returns the ancestor DOM
element matching the selector.

Finally, `get-state` provides a convenient way to check that the state
of the component has been modified correctly.

Here is an example of testing that a component increments a value
after a button has been clicked:

```clojure
(deftest ^:async state-changes-on-click
  (let [rendered-component (huh/rendered-component stateful
                                                   (huh/setup-state {}))]
    (huh/after-event
     :click #js {:target #js {}}
     (huh/in rendered-component "button")
     (fn [_]
       (is (= 2 (:value (huh/get-state rendered-component))))
       (done)))))
```

Since the component's internal state is more of an implementation
detail, it is probably better practice to test that some action takes
place after an event. For example: "After :change to username field,
and :change to password field, and :click to login button, a POST is
made to the login route."

It would probably be helpful for `huh` to provide a simple mechanism
of chaining multiple actions together to simply this testing
scenario. In the meantime, one could probably be implemented
relatively simply using `core.async`.
