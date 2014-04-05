;; Copyright (c) John P. Hackworth. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-cjdns.core 
  (:require [cheshire.core :refer :all]
    [aleph.udp :refer :all]
    [bencode.core :refer :all]
    [gloss.core :refer :all]
    [pandect.core :refer :all]
    [lamina.core :refer :all]
    [clojure.java.io :refer :all]))

;;; Configuration

(def config (atom nil))

(defn read-config! 
  "Reads a JSON-formatted config file at either $HOME/.cjdnsadmin or path specified. 
  
  Example config file: 
  {
   \"addr\":\"127.0.0.1\",
   \"port\":11234, 
   \"password\":\"abcd\"
 }

 Get the address, port and password values from your cjdroute.conf in the \"admin\" section.
 "
 [& [config-path]] 
 (let [config-path (if-not (nil? config-path) 
  config-path 
  (->> (System/getProperty "user.home")
   (format "%s/.cjdnsadmin")))]
 (if-not (.exists (as-file config-path))
  (-> "Error: config file at %s does not exist"
   (format config-path)
   (Exception.)
   (throw))
  (reset! config (parse-string (slurp config-path) true)))))

;;; Network Utility Functions

(defn send! [message] 
  (let [sock @(udp-socket {:frame (string :utf-8)})
    msg {:host (:addr @config)
     :port (:port @config)}]
     (enqueue sock (assoc msg :message (bencode message)))
     (:message @(read-channel sock))))

(defn get-hash [cookie] (sha256 (str (:password @config) cookie)))

(defn get-cookie []
  (let [msg {:q "cookie"}]
    (:cookie (bdecode (send! msg)))))

(defn sign-request 
  [raw-request]
  (let [cookie (get-cookie)
    request (-> raw-request 
      (assoc :hash (get-hash cookie)) 
      (assoc :cookie cookie))
    request-hash (sha256 (bencode request))]
    (assoc request :hash request-hash)))

;;; Public Functions 

(defn public-request 
  [query]
  (if (nil? @config)
    (-> "Error: No config available. You must (read-config!) first"
     (Exception.)
     (throw))) 
  (-> {:q query}
    (send!)
    (bdecode)))

(defn ping [] (public-request "ping"))

(defn memory [] (public-request "memory"))

;;; Admin Functions

(defn auth-request
  [query & [params]]
  (if (nil? @config)
    (-> "Error: No config available. You must (read-config!) first"
     (Exception.)
     (throw))) 
  (-> {:q "auth" :aq query}
    (merge params)
    (sign-request)
    (send!)
    (bdecode)))

(defn looped-auth-request [f k]
 (let [page 0]
  (loop [page page 
   results (auth-request f {:args {:page page}})
   all-results (k results)]
   (if (:more results)
    (recur (inc page) (auth-request f {:args {:page page}}) (into [] (concat all-results (k results))))
    all-results)))) 

(defn admin-available-functions [] 
  (looped-auth-request "Admin_availableFunctions" :availableFunctions))

(defn auth-ping [] (auth-request "ping"))

(defn async-enabled? [] (auth-request "Admin_asyncEnabled"))

(defn exit! [] (auth-request "Core_exit"))

(defn add-password [password user]
  (auth-request "AuthorizedPasswords_add" {:args {:user user 
    :password password}}))

(defn list-users [] (auth-request "AuthorizedPasswords_list"))

(defn remove-user [user] (auth-request "AuthorizedPasswords_remove" {:args {:user user}}))

(defn disconnect-peer [pubkey] 
  (auth-request "InterfaceController_disconnectPeer" {:args {:pubkey pubkey}}))

(defn switch-ping [path & [{:keys [data timeout] :as params}]] 
  (let [args {:args (-> {:path path}
    (merge params))}]
  (auth-request "SwitchPinger_ping" args)))

(defn node-ping 
  "Pings a remote cjdns node. 
  
  node-path may be either a route, a cjdns IP, or a cjdns IP via a specified route.
  
  See: https://github.com/cjdelisle/cjdns/tree/master/admin#routermodule_pingnode for examples."
  [node-path & [timeout]]
  (let [args (if-not (nil? timeout) 
   {:args {:path node-path :timeout timeout}}
   {:args {:path node-path}})]
  (auth-request "RouterModule_pingNode" args)))

(defn peer-stats []
  (looped-auth-request "InterfaceController_peerStats" :peers))

(defn dump-table [] 
  (looped-auth-request "NodeStore_dumpTable" :routingTable))






