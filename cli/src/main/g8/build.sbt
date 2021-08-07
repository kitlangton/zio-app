import BuildEnvPlugin.autoImport

import java.io.InputStream

name := "$name$"
description := "$description$"
version := "0.0.1"

val animusVersion        = "0.1.9"
val boopickleVerison     = "1.3.2"
val laminarVersion       = "0.13.1"
val laminextVersion      = "0.13.10"
val postgresVersion      = "42.2.23"
val quillZioVersion      = "3.9.0"
val sttpVersion          = "3.3.13"
val zioAppVersion        = "0.2.6"
val zioConfigVersion     = "1.0.6"
val zioHttpVersion       = "1.0.0.0-RC17"
val zioJsonVersion       = "0.1.5"
val zioMagicVersion      = "0.3.6"
val zioVersion           = "1.0.10"
val scalaJavaTimeVersion = "2.3.0"
val shoconVersion        = "1.0.0"

val sharedSettings = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  scalacOptions ++= Seq("-Xfatal-warnings"),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
  ),
  libraryDependencies ++= Seq(
    "io.github.kitlangton"           %% "zio-app"     % zioAppVersion,
    "io.suzaku"                     %%% "boopickle"   % boopickleVerison,
    "dev.zio"                       %%% "zio"         % zioVersion,
    "dev.zio"                       %%% "zio-streams" % zioVersion,
    "dev.zio"                       %%% "zio-macros"  % zioVersion,
    "dev.zio"                       %%% "zio-test"    % zioVersion % Test,
    "dev.zio"                       %%% "zio-json"    % zioJsonVersion,
    "io.github.kitlangton"          %%% "zio-app"     % zioAppVersion,
    "com.softwaremill.sttp.client3" %%% "core"        % sttpVersion
  ),
  scalacOptions ++= Seq("-Ymacro-annotations", "-Xfatal-warnings", "-deprecation"),
  scalaVersion := "2.13.6",
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val backend = project
  .in(file("backend"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    sharedSettings,
    Compile / run / mainClass := Some("$package$.Backend"),
    libraryDependencies ++= Seq(
      "io.github.kitlangton"          %% "zio-magic"              % zioMagicVersion,
      "dev.zio"                       %% "zio-config"             % zioConfigVersion,
      "dev.zio"                       %% "zio-config-typesafe"    % zioConfigVersion,
      "dev.zio"                       %% "zio-config-magnolia"    % zioConfigVersion,
      "io.d11"                        %% "zhttp"                  % zioHttpVersion,
      "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % sttpVersion,
      "org.postgresql"                 % "postgresql"             % postgresVersion,
      "io.getquill"                   %% "quill-jdbc-zio"         % quillZioVersion
    )
  )
  .dependsOn(shared)

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin, ShoconPlugin)
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.github.kitlangton" %%% "animus"          % animusVersion,
      "com.raquo"            %%% "laminar"         % laminarVersion,
      "io.github.cquiroz"    %%% "scala-java-time" % scalaJavaTimeVersion,
      "io.laminext"          %%% "websocket"       % laminextVersion,
      "org.akka-js"          %%% "shocon"          % shoconVersion
    )
  )
  .settings(sharedSettings)
  .settings(
    (Compile / compile) := (Compile / compile).dependsOn(shoconConcat).value,
    shoconConcatFile := {
      autoImport.buildEnv.value match {
        case BuildEnv.Production =>
          (Compile / packageBin / artifactPath).value / "scala-2.13/frontend-opt/shocon.conf"
        case _ =>
          (Compile / packageBin / artifactPath).value / "scala-2.13/shocon.conf"
      }
    },
    shoconFilter := {
      autoImport.buildEnv.value match {
        case BuildEnv.Production =>
          tuple: (String, InputStream) => tuple._1.contains("resources/prod")
        case _ =>
          tuple: (String, InputStream) => tuple._1.contains("resources/dev")
      }
    }
  )
  .dependsOn(shared)

lazy val shared = project
  .enablePlugins(ScalaJSPlugin)
  .in(file("shared"))
  .settings(
    sharedSettings,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) }
  )
