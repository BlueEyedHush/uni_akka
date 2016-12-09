
lazy val root = (project in file(".")).
  settings(
    name := "reactive-project",
    version := "0.1",
    scalaVersion := "2.12.0",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.12",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.12",
      "com.typesafe.akka" %% "akka-remote" % "2.4.12",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "ch.qos.logback" % "logback-classic" % "1.1.7"
    )
  )
