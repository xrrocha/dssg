val scala3Version = "3.0.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dssg",
    version := "1.0",

    scalaVersion := scala3Version,
  )
