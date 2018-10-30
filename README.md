Couchbase Plugins for Akka Persistence
======================================

[![Gitter](https://badges.gitter.im/Product-Foundry/akka-persistence-couchbase.svg)](https://gitter.im/Product-Foundry/akka-persistence-couchbase?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![License](https://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Build Status](https://travis-ci.org/Product-Foundry/akka-persistence-couchbase.svg?branch=master)](https://travis-ci.org/Product-Foundry/akka-persistence-couchbase)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/08a13960705442a4ae6b45471d38ee40)](https://www.codacy.com/app/Product-Foundry/akka-persistence-couchbase)
[![Codacy Coverage](https://api.codacy.com/project/badge/coverage/08a13960705442a4ae6b45471d38ee40)](https://www.codacy.com/app/Product-Foundry/akka-persistence-couchbase)
[![Download](https://api.bintray.com/packages/productfoundry/maven/akka-persistence-couchbase/images/download.svg) ](https://bintray.com/productfoundry/maven/akka-persistence-couchbase/_latestVersion)

Replicated [Akka Persistence](http://doc.akka.io/docs/akka/2.5.3/scala/persistence.html) journal and snapshot store backed by [Couchbase](http://www.couchbase.com/).

Dependencies
------------

### Latest release

To include the latest release of the Couchbase plugins into your `sbt` project, add the following lines to your `build.sbt` file:

    resolvers += "Product-Foundry at bintray" at "http://dl.bintray.com/productfoundry/maven"

    libraryDependencies += "com.productfoundry" %% "akka-persistence-couchbase" % "0.8"

This version of `akka-persistence-couchbase` depends on Akka 2.5.3 and Scala 2.11.11.

It is tested on Travis using Couchbase 3.0.1

Journal plugin
--------------

### Features

- All operations required by the Akka Persistence [journal plugin API](http://doc.akka.io/docs/akka/2.5.3/scala/persistence.html#journal-plugin-api) are fully supported.
- The plugin uses Couchbase in a pure log-oriented way i.e. data are only ever inserted but never updated (deletions are made using deletion markers, rather than performing physical deletion).
- Batches are stored in a single Couchbase message to maintain atomicity.

### Configuration

To activate the journal plugin, add the following line to your Akka `application.conf`:

    akka.persistence.journal.plugin = "couchbase-journal"

This will run the journal with its default settings. The default settings can be changed with the configuration properties defined in [reference.conf](https://github.com/Product-Foundry/akka-persistence-couchbase/blob/master/src/main/resources/reference.conf).

### Caveats

- Not yet tested under very high loads.

Snapshot store plugin
---------------------

### Features

- Implements the Akka Persistence [snapshot store plugin API](http://doc.akka.io/docs/akka/2.5.3/scala/persistence.html#snapshot-store-plugin-api).

### Configuration

To activate the snapshot-store plugin, add the following line to your Akka `application.conf`:

    akka.persistence.snapshot-store.plugin = "couchbase-snapshot-store"

This will run the snapshot store with its default settings. The default settings can be changed with the configuration properties defined in [reference.conf](https://github.com/Product-Foundry/akka-persistence-couchbase/blob/master/src/main/resources/reference.conf).


Acknowledgements
----------------

Inspired by [Cassandra Plugins for Akka Persistence](https://github.com/akka/akka-persistence-cassandra).
