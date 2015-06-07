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

See the `test/integration_tests.cljs` for examples of testing a simple
application.
