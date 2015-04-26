(defproject huh "0.9.0"
  :description "A UnitTest assertion library for Om"
  :url "https://github.com/spiralman/huh"
  :license {:name "MIT"
            :url "https://raw.githubusercontent.com/spiralman/huh/master/LICENSE"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]
                 [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.12.2-4"]]
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
