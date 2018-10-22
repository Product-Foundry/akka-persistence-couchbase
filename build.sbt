organization := "com.productfoundry"

name := "akka-persistence-couchbase"

version := "0.7.4"

scalaVersion := "2.11.8"

fork in Test := true

javaOptions in Test += "-Xmx512M"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

parallelExecution in Test := false

enablePlugins(SbtOsgi)

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization := Some("productfoundry")

val akkaVer = "2.5.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % akkaVer,
  "com.couchbase.client" % "java-client" % "2.4.7",
  "commons-codec" % "commons-codec" % "1.10",
  "com.typesafe.akka" %% "akka-persistence-tck" % akkaVer % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.osgi" % "org.osgi.core" % "5.0.0" % "provided"
)

def akkaImport(packageName: String = "akka.*") = versionedImport(packageName, "2.4", "2.5")
def configImport(packageName: String = "com.typesafe.config.*") = versionedImport(packageName, "1.3.0", "1.4.0")
def versionedImport(packageName: String, lower: String, upper: String) = s"""$packageName;version="[$lower,$upper)""""

osgiSettings
OsgiKeys.exportPackage := Seq("akka.persistence.couchbase.*")
OsgiKeys.importPackage := Seq(akkaImport(), "*");
