(defproject huh "0.9.0"
  :description "A UnitTest assertion library for Om"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :cljsbuild {
              :builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "out/huh-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit" ["phantom-js" :runner
                                      "huh-test.js"]}})
