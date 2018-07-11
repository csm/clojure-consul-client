(ns consul.proto.kv
  "Interface to com.orbitz.consul.KeyValueClient.")

(defprotocol IKeyValue
  (get-value [this key] [this key opts]
    "Fetch a value by key.")
  (get-values [this key] [this key opts]
    "Fetch all values for a key prefix.")
  (put-value! [this key value] [this key value opts]
    "Put a value to a key.")
  (get-keys [this key]
    "Get all child keys of a key.")
  (delete-key! [this key]
    "Delete a key.")
  (delete-keys! [this key]
    "Delete all keys for a key prefix.")
  (acquire-lock [this key session] [this key session value]
    "Acquire a lock on a key.")
  (get-session [this key]
    "Get the current locked session for a key.")
  (release-lock [this key session]
    "Release a lock on a key.")
  (transact! [this operations] [this operations options]
    "Perform a transaction.

    operations can either be com.orbitz.consul.model.kv.Operation instances,
    or map specs to be passed to consul.core/operation.

    options can either be a com.orbitz.consul.option.TransactionOptions instance,
    or a map spec to be passed to consul.core/transaction-options"))