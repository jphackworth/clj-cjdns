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
    [flatland.useful.map :refer :all]
    [gloss.core :refer :all]
    [pandect.core :refer :all]
    [clojure.string :as str]
    [lamina.core :refer :all]
    [clojure.java.io :refer :all])
  (:import java.net.Inet6Address 
           java.net.UnknownHostException))

;;; Configuration

(def config (atom nil))
(def crashey-enabled (atom false))

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

(defn crashey-enabled? [] 
  (if (false? @crashey-enabled) 
    (-> "Error: This function requires the crashey branch of cjdns. Set crashey-enabled to true use this: (reset! crashey-enabled true)"
        (Exception.)
        (throw))))

(def loop-forever (comp doall repeatedly))

; Following exception macros from: http://bitumenframework.blogspot.fi/2011/01/non-breaking-error-handling-in-clojure.html

(defmacro filter-exception
  "Execute body of code and in case of an exception, ignore it if (pred ex)
  returns false (i.e. rethrow if true) and return nil."
  [pred & body]
  `(try ~@body
     (catch Exception e#
       (when (~pred e#)
         (throw e#)))))

(defmacro with-exceptions
  "Execute body of code in the context of exceptions to be re-thrown or ignored.
  Args:
    throw-exceptions - List of exceptions that should be re-thrown
    leave-exceptions - List of exceptions that should be suppressed
  Note: 'throw-exceptions' is given preference over 'leave-exceptions'
  Example usage:
    ;; ignore all runtime exceptions except
    ;; IllegalArgumentException and IllegalStateException
    (with-exceptions [IllegalArgumentException IllegalStateException] [RuntimeException]
      ...)"
  [throw-exceptions leave-exceptions & body]
  `(filter-exception (fn [ex#]
                       (cond
                         (some #(instance? % ex#) ~throw-exceptions) true
                         (some #(instance? % ex#) ~leave-exceptions) false
                         :else true))
     ~@body))

(defn resolve-hostname 
  [^String hostname]
  (.getHostAddress (Inet6Address/getByName hostname)))

(defn resolvable? 
  [^String hostname] 
  (boolean (with-exceptions [] [UnknownHostException] (resolve-hostname hostname))))                

;;; Path Validation / Regex
; [0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]
(def cjdns-ipv6-pattern-string (str "(fc[0-9a-fA-F][0-9a-fA-F]):" (str/join ":" (repeat 7 "([0-9a-fA-F]{2,4})"))))
(def       ipv6-pattern-string (str/join ":" (repeat 8 "([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])")))
(def      route-pattern-string (str/join "." (repeat 4 "([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])")))

(def   cjdns-ipv6-pattern (re-pattern (str "(?m)^" cjdns-ipv6-pattern-string "$")))
(def         ipv6-pattern (re-pattern (str "(?m)^" ipv6-pattern-string "$")))
(def cjdns-route-pattern (re-pattern (str "(?m)^" route-pattern-string "$")))
(def       ippath-pattern (re-pattern (str "(?m)^" (str cjdns-ipv6-pattern-string "@" route-pattern-string "$"))))

(defn cjdns-ipv6-addr? [^String v] (boolean (re-seq cjdns-ipv6-pattern v)))
(defn       ipv6-addr? [^String v] (boolean (re-seq ipv6-pattern v)))
(defn     cjdns-route? [^String v] (boolean (re-seq cjdns-route-pattern v)))
(defn          ippath? [^String v] (boolean (re-seq ippath-pattern v)))
(defn cjdns-ipv6-host? [^String v] (boolean (re-seq cjdns-ipv6-pattern (resolve-hostname v))))
              
(defn valid-path? 
  "Validate that the path is one of four possible options: 
  
  1. cjdns IPv6 address of the form: fcXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX
  2. cjdns route of the form: XXXX.XXXX.XXXX.XXXX
  3. cjdns IPv6 with a specific route of the form: fcXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX@XXXX.XXXX.XXXX.XXXX
  4. A hostname that resolves to a cjdns IPv6 address 
  
  These are sequentially tested. 
  
  Exceptions are thrown in the following cases:
  
  1. A hostname doesn't resolve 
  2. A hostname resolves to an IPv4 address
  3. A hostname resolves to a standard IPv6 address 
  "
  [^String v] 
  (if-let [path (cond 
   (cjdns-ipv6-addr? v) v ; 
       (cjdns-route? v) v ; 
            (ippath? v) v ;
   (and (resolvable? v) (cjdns-ipv6-addr? (resolve-hostname v))) (resolve-hostname v))]
    path
    false))  
                                   
;;; Network Utility Functions
                                   
(defn send! [message & [{:keys [async? socket] :as params}]] 
  (let [socket (if-not (nil? socket) socket @(udp-socket {:frame (string :utf-8)}))
        msg {:host (:addr @config) :port (:port @config)}]
     (enqueue socket (assoc msg :message (bencode message)))
     (if async? 
       socket 
       (:message @(read-channel socket)))))

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

(defn looped-auth-request [admin-function k f]
 (let [page 0]
  (loop [page page 
         results (auth-request admin-function {:args {:page page}})
         all-results (k results)]
   (if (:more results)
    (recur (inc page) (auth-request admin-function {:args {:page page}}) (f all-results (k results)))
    all-results)))) 

(defn admin-available-functions [] 
  (looped-auth-request "Admin_availableFunctions" :availableFunctions #(merge %1 %2)))

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
  (if (cjdns-route? path) 
    (let [args (-> {:path path :data data :timeout timeout} 
                   (remove-vals nil?))]
      (auth-request "SwitchPinger_ping"{:args args}))
    (-> "Error: Invalid route supplied."
        (format) 
        (Exception.) 
        (throw))))

(defn node-ping 
  "Pings a remote cjdns node. 
  
  node-path may be either a route, a cjdns IP, or a cjdns IP via a specified route.
  
  See: https://github.com/cjdelisle/cjdns/tree/master/admin#routermodule_pingnode for examples."
  [path & [timeout]]
  (if-let [path (valid-path? path)]
    (let [args (-> {:path path :timeout timeout} (remove-vals nil?))]
      (auth-request "RouterModule_pingNode" {:args args})) 
    (-> "Error: Invalid path supplied."
        (format) 
        (Exception.) 
        (throw))))

(defn peer-stats []
  (looped-auth-request "InterfaceController_peerStats" :peers #(into [] (concat %1 %2))))

(defn get-peers [path & [{:keys [nearby-path timeout] :as options}]]
  (crashey-enabled?)
  (if-let [path (valid-path? path)]
    (let [args (-> {:path path :nearbyPath nearby-path :timeout timeout} 
                   (remove-vals nil?))]
      (auth-request "RouterModule_getPeers" {:args args}))
    (-> "Error: Invalid path supplied."
        (format)
        (Exception.)
        (throw))))

; (defn lookup-address [addr]
;   (crashey-enabled?)
;   (if (cjdns-ipv6-addr? addr)
;     (auth-request "RouterModule_lookup" {:args {:address addr}})
;     (-> "Error: Invalid cjdns IPv6 address"
;         (format)
;         (Exception.)
;         (throw))))

(defn dump-table [] 
  (looped-auth-request "NodeStore_dumpTable" :routingTable #(into [] (concat %1 %2))))


(defn admin-log-subscribe
  "admin-log-subscribe creates a subscription request to the cjdns router. 
  
  It returns an array map containing the background ping process which keeps the logs alive, 
  as well as the channel to read log messages from.
  
  "
  [{:keys [file level line] :as options}]
   (if (nil? @config)
    (-> "Error: No config available. You must (read-config!) first"
     (Exception.)
     (throw))) 
   
  (let [query {:q "auth" :aq "AdminLog_subscribe"}
        args (-> {:file file :level level :line line}
                 (remove-vals nil?))
        message (->> {:args args}
                     (merge query)
                     (sign-request)) 
        ch (send! message {:async? true})]
    
     {:ping (future (loop-forever (fn [] (Thread/sleep 2000) (send! {:q "ping"} {:socket ch}))))
      :channel ch
      :stream-id (:streamId (bdecode (:message @(read-channel ch))))}))

(defn admin-log-unsubscribe 
  [^String stream-id]
  (auth-request "AdminLog_unsubscribe" {:args {:streamId stream-id}}))





