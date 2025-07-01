name := "HealthCareScala"
version := "0.1.0"
scalaVersion := "3.3.6"

// Version constants (matching Chimp project)
val circeV = "0.14.14"
val tapirV = "1.11.35"
val sttpV = "4.0.9"
val zioV = "2.0.15"

libraryDependencies ++= Seq(
  // ZIO dependencies
  "dev.zio" %% "zio" % zioV,
  "dev.zio" %% "zio-streams" % zioV,
  "dev.zio" %% "zio-http" % "3.0.0-RC2",
  "dev.zio" %% "zio-config" % "4.0.0-RC16",
  "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC16",
  
  // Chimp MCP
  "com.softwaremill.chimp" %% "core" % "0.1.2",
  
  // Tapir
  "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % tapirV,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirV,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirV,
  
  // STTP client for HTTP requests (use sync backend to avoid ZIO conflicts)
  "com.softwaremill.sttp.client4" %% "core" % sttpV,
  "com.softwaremill.sttp.client4" %% "circe" % sttpV,
  
  // Circe for JSON
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  
  // Logging (Java 8 compatible versions)
  "ch.qos.logback" % "logback-classic" % "1.2.12",
  "org.slf4j" % "slf4j-api" % "1.7.36"
)

// Dependency overrides to resolve conflicts
dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0"
)

// Scala compiler options
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked"
)