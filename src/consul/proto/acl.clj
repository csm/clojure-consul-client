(ns consul.proto.acl
  "Interface to com.orbitz.consul.AclClient.")

(defprotocol IAcl
  (create-acl [this args] "Create an ACL. Returns info about the new ACL.

  args can either be a instance of com.orbitz.consul.model.acl.AclToken, or a map
  containing keys:

  * :id    The ID of the ACL, a string.
  * :name  The name of the ACL, a string.
  * :type  The type of the ACL, a string.
  * :rules The rules of the ACL, a string.")
  (update-acl [this args] "Update an ACL. Takes the same arguments as create-acl.")
  (destroy-acl [this id] "Destroy an ACL. The argument is the ID of the ACL.")
  (acl-info [this id] "Fetch info about an ACL. The argument is the ID of the ACL. Returns a list of maps giving the ACL info.")
  (clone-acl [this id] "Clone an ACL. The argument is the ID of the ACL, and returns the ID of the cloned ACL.")
  (list-acls [this] "List all ACLs. Returns a list of maps giving the ACLs."))