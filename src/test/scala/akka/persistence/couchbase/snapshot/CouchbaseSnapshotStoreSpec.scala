package akka.persistence.couchbase.snapshot

import akka.persistence.couchbase.support.CouchbasePluginSpec
import akka.persistence.snapshot.SnapshotStoreSpec

class CouchbaseSnapshotStoreSpec extends SnapshotStoreSpec(CouchbasePluginSpec.config) with CouchbasePluginSpec