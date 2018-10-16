package akka.persistence.couchbase

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.couchbase.client.java._
import com.couchbase.client.java.auth.PasswordAuthenticator
import com.couchbase.client.java.env.CouchbaseEnvironment
import com.couchbase.client.java.view.Stale
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration

trait CouchbasePluginConfig {

  def stale: Stale

  def persistTo: PersistTo

  def replicateTo: ReplicateTo

  def timeout: FiniteDuration

  def nodes: java.util.List[String]

  def bucketName: String

  def username: Option[String]

  def bucketPassword: Option[String]
}

abstract class DefaultCouchbasePluginConfig(config: Config) extends CouchbasePluginConfig {

  private val bucketConfig: Config = config.getConfig("bucket")

  override val stale: Stale = Some(config.getString("stale")).flatMap(identifier => Stale.values().find(_.identifier() == identifier)).getOrElse(throw new IllegalArgumentException("Stale property invalid"))

  override val persistTo: PersistTo = PersistTo.valueOf(config.getString("persist-to"))

  override val replicateTo: ReplicateTo = ReplicateTo.valueOf(config.getString("replicate-to"))

  override val timeout: FiniteDuration = FiniteDuration(config.getDuration("timeout").getSeconds, TimeUnit.SECONDS)

  override val nodes: java.util.List[String] = bucketConfig.getStringList("nodes")

  override val bucketName: String = bucketConfig.getString("bucket")

  override val username: Option[String] = Some(bucketConfig.getString("username")).filter(_.nonEmpty)

  override val bucketPassword: Option[String] = Some(bucketConfig.getString("password")).filter(_.nonEmpty)

  private[couchbase] def createCluster(environment: CouchbaseEnvironment): Cluster = {
    CouchbaseCluster.create(environment, nodes)
  }

  private[couchbase] def openBucket(cluster: Cluster): Bucket = {
    username match {
      case None =>
        cluster.openBucket(bucketName, bucketPassword.orNull)
      case Some(user) =>
        cluster.authenticate(new PasswordAuthenticator(user, bucketPassword.orNull))
        cluster.openBucket(bucketName)
    }

  }
}

trait CouchbaseEnvironmentConfig extends CouchbasePluginConfig {

  def connectTimeout: FiniteDuration
  
  def socketConnectTimeout: FiniteDuration

  def openBucketRetryTimeout: FiniteDuration
}

class DefaultCouchbaseEnvironmentConfig(config: Config)
  extends DefaultCouchbasePluginConfig(config)
    with CouchbaseEnvironmentConfig {
  
  override val connectTimeout = FiniteDuration(config.getDuration("connect-timeout").getSeconds, TimeUnit.SECONDS)

  override val socketConnectTimeout = FiniteDuration(config.getDuration("socket-connect-timeout").getSeconds, TimeUnit.SECONDS)

  override val openBucketRetryTimeout = FiniteDuration(config.getDuration("open-bucket-retry-timeout").getSeconds, TimeUnit.SECONDS)
}

object CouchbaseEnvironmentConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseEnvironmentConfig(system.settings.config.getConfig("couchbase-environment"))
  }
}

trait CouchbaseJournalConfig extends CouchbasePluginConfig {

  def replayDispatcherId: String

  def maxMessageBatchSize: Int

  def tombstone: Boolean

}

class DefaultCouchbaseJournalConfig(config: Config)
  extends DefaultCouchbasePluginConfig(config)
    with CouchbaseJournalConfig {

  override val replayDispatcherId = config.getString("replay-dispatcher")

  override val maxMessageBatchSize = config.getInt("max-message-batch-size")

  override val tombstone = config.getBoolean("tombstone")
}

object CouchbaseJournalConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseJournalConfig(system.settings.config.getConfig("couchbase-journal"))
  }
}

trait CouchbaseSnapshotStoreConfig extends CouchbasePluginConfig

class DefaultCouchbaseSnapshotStoreConfig(config: Config)
  extends DefaultCouchbasePluginConfig(config)
    with CouchbaseSnapshotStoreConfig

object CouchbaseSnapshotStoreConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseSnapshotStoreConfig(system.settings.config.getConfig("couchbase-snapshot-store"))
  }
}