# clj-cjdns

Library for communicating with [cjdns](https://github.com/cjdelisle/cjdns) [admin RPC interface](https://github.com/cjdelisle/cjdns/tree/master/admin)

This is library provides support for the several of the admin functions.

## Leiningen Project Dependency

[clj-cjdns "0.1.3"]

# Usage

## Crashey support

I've begun adding admin functions that are only available in the [crashey branch of cjdns](https://github.com/cjdelisle/cjdns/tree/crashey). 

To use these functions, you must set the crashey-enabled atom to true, otherwise an exception will be thrown.

An example of a new admin function is get-peers:

```clojure
user=> (use 'clj-cjdns.core 'clojure.pprint)
nil
user=> (pprint (get-peers "fcf1:4c00:7171:d61a:d4ce:3a5d:ea79:ad3e"))

Exception Error: This function requires the crashey branch of cjdns. Set crashey-enabled to true use this: (reset! crashey-enabled true)  clj-cjdns.core/crashey-enabled? (core.clj:49)
user=> (reset! crashey-enabled true)
user=> (pprint (get-peers "fcf1:4c00:7171:d61a:d4ce:3a5d:ea79:ad3e"))
{:error "none",
:ms 364,
:peers
["v2.0000.0000.0082.9233.lkmcfq0qd6nsdls2826p80r82854ypwzzj4mxtj41xdfd3z2s570.k"
"v5.0000.0000.0083.1233.qvnz262dnhm05m3unkbz27uwzx8lkup6m9mhc3gk6zjhky8dy8m0.k"
"v6.0000.0000.0083.9233.my455p859j9zhrgzwgg4w8k2uj2y3yfl1km6my2ut838b82gln30.k"
"v6.0000.0000.0084.1233.23d9qmlw59rv0d9yusgh9ltgszy4hz1m54yp342rn3sfntu4ns70.k"
"v6.0000.0000.0084.9233.8hgr62ylugxjyyhxkz254qtz60p781kbswmhhywtbb5rpzc5lxj0.k"
"v6.0000.0000.0085.1233.2z5lkm9d7c8rbc77qty33sfv050mj5fpc6wjfbbqjyb7kxbrxvj0.k"
"v6.0000.0000.0086.1233.jufvunvsmy08r69dhhw1w170d26qj5kvybdcf45jbkrtw7pb7cl0.k"
"v6.0000.0000.0086.9233.nhmsdzjv6p2gmmwgl8tjlxkj2y0l1b382ur67ppgvnw3pxs96zf0.k"],
:result "peers"}
nil
```

Crashey admin function support is minimal at this stage. More coming in future versions.

## Configuration

A configuration file must be loaded via the (read-config!) function. It looks by default in $HOME/.cjdnsadmin or the path specified.

Before you can make any requests, you must read-config! first.

After reading the config file, the configuration parameters are in the @config atom, as demonstrated below. 

```clojure
user=> (use 'clj-cjdns.core)
nil
user=> (doc read-config!)
-------------------------
clj-cjdns.core/read-config!
([& [config-path]])
  Reads a JSON-formatted config file at either $HOME/.cjdnsadmin or path specified. 
  
  Example config file: 
  {
   "addr":"127.0.0.1",
   "port":11234, 
   "password":"abcd"
 }

 Get the address, port and password values from your cjdroute.conf in the "admin" section.
 
nil
user=> @config
nil
user=> (read-config!)
{:addr "127.0.0.1", :port 11234, :password "abcd"}
user=> @config
{:addr "127.0.0.1", :port 11234, :password "abcd"}
```

## Public Functions

Public functions do not require a password.

At present, the following public RPC functions are supported.

### (ping)

Checks if admin connection is alive.

Reference: [ping()](https://github.com/cjdelisle/cjdns/tree/master/admin#ping)

```clojure
user=> (ping)
{:q "pong"}
```

### (memory)

Checks memory used (in bytes) my router.

Reference: [memory()](https://github.com/cjdelisle/cjdns/tree/master/admin#memory)

```clojure
user=> (memory)
{:bytes 1207080}
```

## Admin Functions

Admin functions require a password. 

At present, the following admin RPC functions are supported.

### (admin-available-functions)

This function lists admin functions available via the RPC interface. Not all of these have matching functions in clj-cjdns. However, you can use the lower-level (auth-request) and (looped-auth-request) in the interim.

Reference: [Admin_availableFunctions()](https://github.com/cjdelisle/cjdns/tree/master/admin#admin_availablefunctions)

```clojure
user=> (clojure.pprint/pprint (admin-available-functions))
{:Admin_asyncEnabled {},
 :Admin_availableFunctions {:page {:required 0, :type "Int"}},
 :AuthorizedPasswords_add
 {:authType {:required 0, :type "Int"},
  :password {:required 1, :type "String"},
  :user {:required 1, :type "String"}},
 :AuthorizedPasswords_list {},
 :AuthorizedPasswords_remove {:user {:required 1, :type "String"}},
 :Core_initTunnel {:desiredTunName {:required 0, :type "String"}},
 :ETHInterface_beacon
 {:interfaceNumber {:required 0, :type "Int"},
  :state {:required 0, :type "Int"}},
 :ETHInterface_beginConnection
 {:interfaceNumber {:required 0, :type "Int"},
  :macAddress {:required 1, :type "String"},
  :password {:required 0, :type "String"},
  :publicKey {:required 1, :type "String"}},
 :ETHInterface_new {:bindDevice {:required 1, :type "String"}},
 :InterfaceController_disconnectPeer
 {:pubkey {:required 1, :type "String"}},
 :InterfaceController_peerStats {:page {:required 0, :type "Int"}},
 :IpTunnel_allowConnection
 {:ip4Address {:required 0, :type "String"},
  :ip6Address {:required 0, :type "String"},
  :publicKeyOfAuthorizedNode {:required 1, :type "String"}},
 :IpTunnel_connectTo
 {:publicKeyOfNodeToConnectTo {:required 1, :type "String"}},
 :IpTunnel_listConnections {},
 :IpTunnel_removeConnection {:connection {:required 1, :type "Int"}},
 :IpTunnel_showConnection {:connection {:required 1, :type "Int"}},
 :NodeStore_dumpTable {:page {:required 1, :type "Int"}},
 :NodeStore_getLink
 {:linkNum {:required 1, :type "Int"},
  :parent {:required 1, :type "String"}},
 :NodeStore_getNode {:ip {:required 0, :type "String"}},
 :NodeStore_getRouteLabel
 {:childAddress {:required 1, :type "String"},
  :pathToParent {:required 1, :type "String"}},
 :RainflyClient_addKey {:ident {:required 1, :type "String"}},
 :RouterModule_lookup {:address {:required 1, :type "String"}},
 :RouterModule_pingNode
 {:path {:required 1, :type "String"},
  :timeout {:required 0, :type "Int"}},
 :SearchRunner_showActiveSearch {:number {:required 1, :type "Int"}},
 :Security_dropPermissions {},
 :Security_setUser {:user {:required 1, :type "String"}},
 :SessionManager_getHandles {:page {:required 0, :type "Int"}},
 :SessionManager_sessionStats {:handle {:required 1, :type "Int"}},
 :SwitchPinger_ping
 {:data {:required 0, :type "String"},
  :path {:required 1, :type "String"},
  :timeout {:required 0, :type "Int"}},
 :UDPInterface_beginConnection
 {:address {:required 1, :type "String"},
  :interfaceNumber {:required 0, :type "Int"},
  :password {:required 0, :type "String"},
  :publicKey {:required 1, :type "String"}},
 :UDPInterface_new {:bindAddress {:required 0, :type "String"}},
 :ping {}}
nil
```

### (async-enabled?) 

Check whether async communication is allowed. This is related to admin logging.

Reference: [Admin_asyncEnabled()](https://github.com/cjdelisle/cjdns/tree/master/admin#admin_asyncenabled)

```clojure
user=> (async-enabled?)
{:asyncEnabled 1}
```

### (exit!)

This function stops cjdns.

Reference: [Core_exit()](https://github.com/cjdelisle/cjdns/tree/master/admin#core_exit)

```clojure
(exit!)
```

### (peer-stats)

This function returns stats for all connected peers, including traffic volume in/out, connection direction, public key, state, and user identifier.

**Important**: These stats are in-memory, accrued from the lifetime of your cjdroute process. When you restart cjdns, these stats will start from 0. 

Reference: InterfaceController_peerStats

```clojure
user=> (clojure.pprint/pprint (peer-stats))
[{:bytesIn 27011,
  :bytesOut 66045,
  :duplicates 0,
  :isIncoming 1,
  :last 1396688748788,
  :lostPackets 0,
  :publicKey "8g8bndw7928ss5tx1tqw46wqz9xux0b1r56jz38zq8hpq28r6un0.k",
  :receivedOutOfRange 0,
  :state "ESTABLISHED",
  :switchLabel "0000.0000.0000.0023",
  :user "Local Peers"}
 {:bytesIn 45800324,
  :bytesOut 46084453,
  :duplicates 0,
  :isIncoming 0,
  :last 1396688748854,
  :lostPackets 21,
  :publicKey "w983vxlkg13qp8lqr2dxy4f85gfqf3883zsvhsdxtrst80qhdy60.k",
  :receivedOutOfRange 0,
  :state "ESTABLISHED",
  :switchLabel "0000.0000.0000.0025"}]
nil
```

### (dump-table)

Dumps the router's routing table.

Reference: [NodeStore_dumpTable()](https://github.com/cjdelisle/cjdns/tree/master/admin#nodestore_dumptable)

```clojure
(dump-table)
```

### (add-password)

Adds a password to the router's live configuration, with a user identifier.

**Important**: This does not persist the password configuration to disk. You must separately add it to cjdroute.conf if you want it available on restart.

Reference: [AuthorizedPasswords_add()](https://github.com/cjdelisle/cjdns/tree/master/admin#authorizedpasswords_add)

Parameters: 

- password: The password to add
- user-identifier: A string identifying the user

```clojure
user=> (add-password "abcd123" "alice")
{:error "none"}
```

### (list-users)

Lists the user identifiers for passwords in the router's live configuration.

Reference: [AuthorizedPasswords_list()](https://github.com/cjdelisle/cjdns/tree/master/admin#authorizedpasswords_list)

```clojure
user=> (list-users)
{:total 3, :users ["alice" "password [0]" "Local Peers"]}
```

### (remove-user)

Removes the password from the router's live configuration based on the user identifier.

Reference: AuthorizedPasswords_remove(user)

Parameter:

- user: The user identifier for the password to remove

```clojure
user=> (list-users)
{:total 3, :users ["alice" "password [0]" "Local Peers"]}
user=> (remove-user "alice")
{:error "none"}
user=> (list-users)
{:total 2, :users ["password [0]" "Local Peers"]}
```

### (disconnect-peer)

Disconnect the peer identified by the specified public key. You can use (peer-stats) to identify connected peers and their public keys.

**Important**: This does not prevent the peer from reconnecting. You must also remove the peer's password (in the live router config and in the on-disk config file) if you want it permanent.

Reference: InterfaceController_disconnectPeer(pubkey)

```clojure
user=> (disconnect-peer "8g8bndw7928ss5tx1tqw46wqz9xux0b1r56jz38zq8hpq28r6un0.k")
{:sucess 1}
```

### (node-ping)

Pings a remote cjdns node.

Parameters:

- node-path: May either be a cjdns IP address, a route, or a cjdns IP via a specific route. See reference below for examples
- timeout (optional): timeout in milliseconds

Reference: [RouterModule_pingNode()](https://github.com/cjdelisle/cjdns/tree/master/admin#routermodule_pingnode)

```clojure
user=> (node-ping "fcbf:7bbc:32e4:716:bd00:e936:c927:fc14")
{:from "fcbf:7bbc:32e4:0716:bd00:e936:c927:fc14@0000.0000.0004.c0c5", :ms 473, :protocol 6, :result "pong"}
user=> (node-ping "fcbf:7bbc:32e4:716:bd00:e936:c927:fc14" 200)
{:ms 201, :result "timeout"}
```

### (switch-ping)

Send a switch-level ping without a routing table lookup.

Reference: [SwitchPinger_ping()](https://github.com/cjdelisle/cjdns/tree/master/admin#switchpinger_ping)

_(switch-ping path & [{:data "abcd" :timeout 2000}])_

```clojure
(switch-ping "0000.0000.04f5.2555")
```

## License

Copyright © 2014 John P. Hackworth

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
