(defproject clj-cjdns "0.1.4"
  :description "Library for communicating with Cjdns Admin interface"
  :url "https://github.com/jphackworth/clj-cjdns"
  :license {:name "Eclipse Public License"
  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.5.1"]
    [bencode "0.2.5"]
    [cheshire "5.3.1"]
    [aleph "0.3.2"]
    [lamina "0.5.2"]
    [pandect "0.3.0"]
    [org.flatland/useful "0.11.2"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]}})
