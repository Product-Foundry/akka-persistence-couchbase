package akka.persistence.couchbase

import java.util.concurrent.TimeUnit

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.Logging
import com.couchbase.client.java.Bucket
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.env.{CouchbaseEnvironment, DefaultCouchbaseEnvironment}
import com.couchbase.client.java.util.Blocking
import com.couchbase.client.java.view.DesignDocument

import scala.util.{Failure, Try}

trait Couchbase extends Extension {

  def environment: CouchbaseEnvironment

  def environmentConfig: CouchbaseEnvironmentConfig

  def journalBucket: Bucket

  def journalConfig: CouchbaseJournalConfig

  def snapshotStoreBucket: Bucket

  def snapshotStoreConfig: CouchbaseSnapshotStoreConfig
}

private class DefaultCouchbase(val system: ExtendedActorSystem) extends Couchbase {

  private val log = Logging(system, getClass.getName)

  override val environmentConfig = CouchbaseEnvironmentConfig(system)

  override val journalConfig = CouchbaseJournalConfig(system)

  override val snapshotStoreConfig = CouchbaseSnapshotStoreConfig(system)

  override val environment = DefaultCouchbaseEnvironment
                              .builder()
                              .connectTimeout(environmentConfig.connectTimeout.toMillis)
                              .socketConnectTimeout(environmentConfig.socketConnectTimeout.toMillis.toInt)
                              .build();

  private val journalCluster = journalConfig.createCluster(environment)

  override val journalBucket = openJournalBucketWithRetry()

  private val snapshotStoreCluster = snapshotStoreConfig.createCluster(environment)

  override val snapshotStoreBucket = openSnapshotStoreBucketWithRetry()

  updateJournalDesignDocs()
  updateSnapshotStoreDesignDocs()

  def shutdown(): Unit = {
    attemptSafely("Closing journal bucket")(journalBucket.close())

    attemptSafely("Shutting down journal cluster")(journalCluster.disconnect())

    attemptSafely("Closing snapshot store bucket")(snapshotStoreBucket.close())
    attemptSafely("Shutting down snapshot store cluster")(snapshotStoreCluster.disconnect())

    attemptSafely("Shutting down environment") {
      Blocking.blockForSingle(environment.shutdownAsync().single(), 30, TimeUnit.SECONDS)
    }
  }

  private def openJournalBucketWithRetry(): Bucket = {
    var bucket: Bucket = null
    while(bucket == null) {
      try {
        bucket = journalConfig.openBucket(journalCluster)
      } catch {
        case ex:  Exception=> {
          log.warning("Could not connect to journal bucket. Retrying in 10 seconds...")
          TimeUnit.SECONDS.sleep(environmentConfig.openBucketRetryTimeout.toSeconds)
        }
      }
    }
    return bucket
  }

  private def openSnapshotStoreBucketWithRetry(): Bucket = {
    var bucket: Bucket = null
    while(bucket == null) {
      try {
        bucket = snapshotStoreConfig.openBucket(snapshotStoreCluster)
      } catch {
        case ex:  Exception=> {
          log.warning("Could not connect to config bucket. Retrying in 10 seconds...")
          TimeUnit.SECONDS.sleep(environmentConfig.openBucketRetryTimeout.toSeconds)
        }
      }
    }
    return bucket
  }

  private def attemptSafely(message: String)(block: => Unit): Unit = {
    log.debug(message)

    Try(block) recoverWith {
      case e =>
        log.error(e, message)
        Failure(e)
    }
  }

  /**
    * Initializes all design documents.
    */
  private def updateJournalDesignDocs(): Unit = {

    val designDocs = JsonObject.create()
      .put("views", JsonObject.create()
        .put("by_sequenceNr", JsonObject.create()
          .put("map",
            """
              |function (doc, meta) {
              |  if (doc.dataType === 'journal-messages') {
              |    var messages = doc.messages;
              |    for (var i = 0, l = messages.length; i < l; i++) {
              |      var message = messages[i];
              |      emit([message.persistenceId, message.sequenceNr], message);
              |    }
              |  }
              |}
            """.stripMargin
          )
        )
      )

    updateDesignDocuments(journalBucket, "journal", designDocs)
  }

  /**
    * Initializes all design documents.
    */
  private def updateSnapshotStoreDesignDocs(): Unit = {

    val designDocs = JsonObject.create()
      .put("views", JsonObject.create()
        .put("by_sequenceNr", JsonObject.create()
          .put("map",
            """
              |function (doc) {
              |  if (doc.dataType === 'snapshot-message') {
              |    emit([doc.persistenceId, doc.sequenceNr], null);
              |  }
              |}
            """.stripMargin
          )
        )
        .put("by_timestamp", JsonObject.create()
          .put("map",
            """
              |function (doc) {
              |  if (doc.dataType === 'snapshot-message') {
              |    emit([doc.persistenceId, doc.timestamp], null);
              |  }
              |}
            """.stripMargin
          )
        )
        .put("all", JsonObject.create()
          .put("map",
            """
              |function (doc) {
              |  if (doc.dataType === 'snapshot-message') {
              |    emit(doc.persistenceId, null);
              |  }
              |}
            """.stripMargin
          )
        )
      )

    updateDesignDocuments(snapshotStoreBucket, "snapshots", designDocs)
  }

  private def updateDesignDocuments(bucket: Bucket, name: String, raw: JsonObject): Unit = {
    Try {
      val designDocument = DesignDocument.from(name, raw)
      bucket.bucketManager.upsertDesignDocument(designDocument)
    } recoverWith {
      case e =>
        log.error(e, "Update design docs with name: {}", name)
        Failure(e)
    }
  }
}

object CouchbaseExtension extends ExtensionId[Couchbase] with ExtensionIdProvider {

  override def lookup(): ExtensionId[Couchbase] = CouchbaseExtension

  override def createExtension(system: ExtendedActorSystem): Couchbase = {
    val couchbase = new DefaultCouchbase(system)
    system.registerOnTermination(couchbase.shutdown())
    couchbase
  }
}
