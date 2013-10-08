name := "smoke"

organization := "com.mdialog"

version := "0.7.0-SNAPSHOT"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.5",
  "io.netty" % "netty" % "3.7.0.Final",
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Some(Resolver.file("Snapshots", file("../mdialog.github.com/snapshots/")))
  else
    Some(Resolver.file("Releases", file("../mdialog.github.com/releases/")))
}
