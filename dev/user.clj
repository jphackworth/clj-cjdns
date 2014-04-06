(ns user
  (:require [cheshire.core :refer :all]
            [aleph.udp :refer :all]
            [lamina.core :refer :all]
            [clj-cjdns.core :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.repl :refer :all]
            [flatland.useful.map :refer :all]
            [bencode.core :refer :all]
            [clojure.pprint :refer :all]
            [pandect.core :refer :all])
  (:import java.net.Inet6Address 
           java.net.UnknownHostException))


(read-config!)
(reset! crashey-enabled true)

(defn bytes-to-megabytes [bytes] (* bytes 9.5367431640625e-07))
(defn bytes-to-gigabytes [bytes] (* bytes 9.31322574615479e-10))

(defn read-logs [level]
  (let [{:keys [ping channel stream-id] :as results} (admin-log-subscribe {:level level})]
    (println stream-id)
    (loop-forever (fn [] (let [log (->> @(read-channel channel)
                                 (:message)
                                 (bdecode))]
                    (println log))))))
                  

