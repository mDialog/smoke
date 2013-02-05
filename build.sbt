name := "smoke"

organization := "com.mdialog"

version := "0.3.5"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.5",
  "io.netty" % "netty" % "3.6.2.Final",
  "com.typesafe.akka" % "akka-actor" % "2.0.3",
  "org.scalatest" %% "scalatest" % "1.7.1" % "test",
  "com.typesafe.akka" % "akka-testkit" % "2.0.1" % "test",
  "com.mdialog" %% "config" % "0.4.0"
)

credentials in ThisBuild += Credentials(Path.userHome / ".mdialog.credentials")

resolvers in ThisBuild ++= Seq(
    "mDialog snapshots" at "http://artifactory.mdialog.com/artifactory/snapshots",
    "mDialog releases" at "http://artifactory.mdialog.com/artifactory/releases",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases"
)

publishTo in ThisBuild <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Some("mDialog snapshots" at "http://artifactory.mdialog.com/artifactory/snapshots")
  else
    Some("mDialog releases" at "http://artifactory.mdialog.com/artifactory/releases")
}

