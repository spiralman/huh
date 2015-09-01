(defproject huh "0.9.4"
  :description "A UnitTest assertion library for Om"
  :url "https://github.com/spiralman/huh"
  :license {:name "MIT"
            :url "https://raw.githubusercontent.com/spiralman/huh/master/LICENSE"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.12.2-4"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [codox "0.8.12"]]
  ;; Codox does not work with latest ClojureScript:
  ;; https://github.com/weavejester/codox/issues/90
  :profiles {:doc {:dependencies [[org.clojure/clojurescript "0.0-2985"]]}}
  :cljsbuild {
              :builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "out/huh-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit" ["phantomjs"
                                      "test.js" "test.html"]}}
  :codox {:language :clojurescript
          :src-dir-uri "http://github.com/spiralman/huh/blob/master/"
          :src-linenum-anchor-prefix "L"
          :defaults {:doc/format :markdown}})
