enablePlugins(GraalVMNativeImagePlugin)

val scala3Version = "3.0.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dssg",
    version := "0.1.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.10" % Test
    )
  )

graalVMNativeImageOptions ++= Seq(
  "--allow-incomplete-classpath",
  "-H:ResourceConfigurationFiles=../../configs/resource-config.json",
  "-H:ReflectionConfigurationFiles=../../configs/reflect-config.json",
  "-H:JNIConfigurationFiles=../../configs/jni-config.json",
  "-H:DynamicProxyConfigurationFiles=../../configs/proxy-config.json"
)