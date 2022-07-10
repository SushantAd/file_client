lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion    = "2.6.19"

fork := true

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inThisBuild(List(
      organization    := "com.demo.file.client",
      scalaVersion    := "2.13.4"
    )),
    name := "file_client",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,

      "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.5",
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % "it, test",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % "it, test",
      "org.scalatest"     %% "scalatest"                % "3.1.4"         % "it, test"
    )
  )
