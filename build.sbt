name := "smoke"

organization := "com.mdialog"

version := "0.0.6-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "3.4.5.Final",
  "com.google.protobuf" % "protobuf-java" % "2.4.1",
  "com.typesafe.akka" % "akka-actor" % "2.0.1",
  "org.scalatest" %% "scalatest" % "1.7.1" % "test",
  "com.typesafe.akka" % "akka-testkit" % "2.0.1" % "test"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT")) 
    Some(Resolver.file("Snapshots", file("../mdialog.github.com/snapshots/")))
  else
    Some(Resolver.file("Releases", file("../mdialog.github.com/releases/")))
}
