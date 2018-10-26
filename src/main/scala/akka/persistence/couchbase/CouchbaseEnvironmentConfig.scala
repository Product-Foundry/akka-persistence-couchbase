package akka.persistence.couchbase

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

trait CouchbaseEnvironmentConfig {

  def kvTimeout: FiniteDuration

  def connectTimeout: FiniteDuration
  
  def socketConnectTimeout: FiniteDuration

  def maxRequestLifetime: FiniteDuration

  def openBucketRetryInterval: FiniteDuration

  def openBucketRetryTimeout: FiniteDuration
}

class DefaultCouchbaseEnvironmentConfig(config: Config) extends CouchbaseEnvironmentConfig {
  
  override val kvTimeout = FiniteDuration(config.getDuration("key-value-timeout").getSeconds, TimeUnit.SECONDS)
  
  override val connectTimeout = FiniteDuration(config.getDuration("connect-timeout").getSeconds, TimeUnit.SECONDS)

  override val socketConnectTimeout = FiniteDuration(config.getDuration("socket-connect-timeout").getSeconds, TimeUnit.SECONDS)

  override val maxRequestLifetime = FiniteDuration(config.getDuration("max-request-lifetime").getSeconds, TimeUnit.SECONDS)

  override val openBucketRetryInterval = FiniteDuration(config.getDuration("open-bucket-retry-interval").getSeconds, TimeUnit.SECONDS)

  override val openBucketRetryTimeout = FiniteDuration(config.getDuration("open-bucket-retry-timeout").getSeconds, TimeUnit.SECONDS)
}

object CouchbaseEnvironmentConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseEnvironmentConfig(system.settings.config.getConfig("couchbase-environment"))
  }
}