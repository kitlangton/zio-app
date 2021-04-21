ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

nativeImageOptions ++= List("--no-fallback", "--allow-incomplete-classpath")

val zioVersion = "1.0.6"

lazy val root = (project in file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "zio-app",
    libraryDependencies ++= Seq(
      "dev.zio"  %% "zio"         % zioVersion,
      "dev.zio"  %% "zio-process" % "0.3.0",
      "dev.zio"  %% "zio-streams" % zioVersion,
      "dev.zio"  %% "zio-test"    % zioVersion % Test,
      "org.jline" % "jline"       % "3.19.0"
    ),
    Compile / mainClass := Some("zio.app.WebRunner"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
