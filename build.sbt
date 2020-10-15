name := "io-comparative"

version := "0.1"

scalaVersion := "2.13.3"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.13.3",
  "org.typelevel" %% "cats-effect" % "2.2.0",
  "io.monix" %% "monix" % "3.2.2",
  "io.monix" %% "monix-bio" % "1.0.0",
  "dev.zio" %% "zio" % "1.0.3",
  "dev.zio" %% "zio-streams" % "1.0.3",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10",
  "com.typesafe.akka" %% "akka-stream" % "2.6.10",
  "co.fs2" %% "fs2-core" % "2.4.4",
  "co.fs2" %% "fs2-io" % "2.4.4"
).map(l => l.withSources())
