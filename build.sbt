
lazy val root = (project in file(".")).
  settings(
    name := "reactive-project",
    version := "0.1",
    scalaVersion := "2.12.0",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.12"
    )
  )
