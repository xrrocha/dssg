enablePlugins(GraalVMNativeImagePlugin)

val scala3Version = "3.0.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dssg",
    version := "1.0",

    scalaVersion := scala3Version,
  )

graalVMNativeImageOptions ++= Seq(
  "--allow-incomplete-classpath",
  "-H:ResourceConfigurationFiles=../../configs/resource-config.json",
  "-H:ReflectionConfigurationFiles=../../configs/reflect-config.json",
  "-H:JNIConfigurationFiles=../../configs/jni-config.json",
  "-H:DynamicProxyConfigurationFiles=../../configs/proxy-config.json"
)