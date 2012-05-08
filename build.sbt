name := "smoke"

organization := "com.mdialog"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "org.jboss.netty" % "netty" % "3.2.7.Final",
  "com.typesafe.akka" % "akka-actor" % "2.0.1",
  "com.typesafe.akka" % "akka-zeromq" % "2.0.1",
  "org.scalatest" %% "scalatest" % "1.7.1" % "test",
  "com.typesafe.akka" % "akka-testkit" % "2.0.1" % "test"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
