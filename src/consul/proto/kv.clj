(ns consul.proto.kv)

(defprotocol IKeyValue
  (get-value [this key] [this key opts])
  (get-values [this key] [this key opts])
  (put-value! [this key value] [this key value opts])
  (get-keys [this key])
  (delete-key! [this key])
  (delete-keys! [this key])
  (acquire-lock [this key session] [this key session value])
  (get-session [this key])
  (release-lock [this key session])
  (transact! [this operations] [this operations options]))