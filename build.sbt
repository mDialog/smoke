name := "smoke"

organization := "com.mdialog"

version := "2.1.5"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.10.4", "2.11.0")

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions"
)

libraryDependencies ++= Seq(
  "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.0.5",
  "io.netty" % "netty" % "3.9.4.Final",
  "com.typesafe" % "config" % "1.2.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Some(Resolver.file("Snapshots", file("../mdialog.github.com/snapshots/")))
  else
    Some(Resolver.file("Releases", file("../mdialog.github.com/releases/")))
}

parallelExecution in Test := false

unmanagedBase <<= baseDirectory { base => base / "test/lib" }

unmanagedJars in Test <<= unmanagedBase  map { base => (base ** "public.jar").classpath }

excludeFilter in unmanagedJars in Compile := "public.jar"

excludeFilter in unmanagedJars in Compile ~= { _ || "public.jar" }