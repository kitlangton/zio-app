import BuildEnvPlugin.autoImport
import BuildEnvPlugin.autoImport.BuildEnv
import xerial.sbt.Sonatype.autoImport.sonatypeCredentialHost

import java.io.InputStream

lazy val scala213 = "2.13.6"
lazy val scala3   = "3.0.0"

inThisBuild(
  List(
    name := "zio-app",
    normalizedName := "zio-app",
    organization := "io.github.kitlangton",
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213),
    homepage := Some(url("https://github.com/kitlangton/zio-app")),
    licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kitlangton",
        "Kit Langton",
        "kit.langton@gmail.com",
        url("https://github.com/kitlangton")
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org"
  )
)

lazy val supportedScalaVersions = List(scala213)

val animusVersion        = "0.1.9"
val boopickleVerison     = "1.3.2"
val fansiVersion         = "0.2.14"
val laminarVersion       = "0.13.1"
val laminextVersion      = "0.13.10"
val postgresVersion      = "42.2.23"
val quillVersion         = "3.9.0"
val scalaJavaTimeVersion = "2.3.0"
val shoconVersion        = "1.0.0"
val sttpVersion          = "3.3.13"
val zioHttpVersion       = "1.0.0.0-RC17"
val zioJsonVersion       = "0.1.5"
val zioMagicVersion      = "0.3.6"
val zioNioVersion        = "1.0.0-RC11"
val zioProcessVersion    = "0.4.0"
val zioVersion           = "1.0.10"
val zioQueryVersion      = "0.2.9"

val sharedSettings = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  scalacOptions ++= Seq("-Xfatal-warnings"),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
  ),
  libraryDependencies ++= Seq(
    "io.suzaku"            %%% "boopickle"   % boopickleVerison,
    "dev.zio"              %%% "zio"         % zioVersion,
    "dev.zio"              %%% "zio-streams" % zioVersion,
    "dev.zio"              %%% "zio-test"    % zioVersion % Test,
    "io.github.kitlangton" %%% "zio-magic"   % zioMagicVersion,
    "com.lihaoyi"          %%% "fansi"       % fansiVersion
  ),
  scalacOptions ++= Seq("-Ymacro-annotations", "-Xfatal-warnings", "-deprecation"),
  scalaVersion := scala213,
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val root = (project in file("."))
  .aggregate(cli, cliFrontend, cliShared, core.jvm, core.js, examples.jvm, examples.js)
  .settings(
    name := "zio-app",
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true,
    welcomeMessage
  )

lazy val cli = (project in file("cli"))
  .enablePlugins(NativeImagePlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "zio-app-cli",
    publish / skip := true,
    nativeImageOptions ++= List(
      "-H:ResourceConfigurationFiles=../../src/main/resources/resource-config.json",
      "--report-unsupported-elements-at-runtime",
      "--verbose",
      "--no-server",
      "--allow-incomplete-classpath",
      "--no-fallback",
      "--install-exit-handlers",
      "-H:+ReportExceptionStackTraces",
      "-H:+RemoveSaturatedTypeFlows",
      "-H:+TraceClassInitialization",
      "--initialize-at-run-time=io.netty.channel.epoll.Epoll",
      "--initialize-at-run-time=io.netty.channel.epoll.Native",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray",
      "--initialize-at-run-time=io.netty.channel.DefaultFileRegion",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
      "--initialize-at-run-time=io.netty.channel.kqueue.Native",
      "--initialize-at-run-time=io.netty.channel.unix.Errors",
      "--initialize-at-run-time=io.netty.channel.unix.IovArray",
      "--initialize-at-run-time=io.netty.channel.unix.Limits",
      "--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger",
      "--initialize-at-run-time=io.netty.util.AbstractReferenceCounted",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
      "--initialize-at-build-time=org.slf4j.LoggerFactory",
      "-H:IncludeResources='.*'"
    ),
    libraryDependencies ++= Seq(
      "dev.zio"  %% "zio-process" % zioProcessVersion,
      "dev.zio"  %% "zio-nio"     % zioNioVersion,
      "io.d11"   %% "zhttp"       % zioHttpVersion,
      "org.jline" % "jline"       % "3.20.0"
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
    ),
    Compile / mainClass := Some("zio.app.Main"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(sharedSettings)
  .dependsOn(cliShared, coreJVM)

lazy val cliFrontend = project
  .in(file("cli-frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSLinkerConfig ~= {
      _.withSourceMap(false)
    },
    publish / skip := true,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.github.kitlangton"          %%% "animus"               % animusVersion,
      "com.raquo"                     %%% "laminar"              % laminarVersion,
      "io.github.cquiroz"             %%% "scala-java-time"      % scalaJavaTimeVersion,
      "io.github.cquiroz"             %%% "scala-java-time-tzdb" % scalaJavaTimeVersion,
      "io.laminext"                   %%% "websocket"            % laminextVersion,
      "com.softwaremill.sttp.client3" %%% "core"                 % sttpVersion,
      "com.softwaremill.sttp.client3" %%% "monix"                % sttpVersion
    )
  )
  .settings(sharedSettings)
  .dependsOn(cliShared, coreJS)

lazy val cliShared = project
  .enablePlugins(ScalaJSPlugin)
  .in(file("cli-shared"))
  .settings(
    sharedSettings,
    publish / skip := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSLinkerConfig ~= {
      _.withSourceMap(false)
    }
  )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(
    name := "zio-app",
    ThisBuild / scalaVersion := scala213,
    crossScalaVersions := supportedScalaVersions,
    publish / skip := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
    ),
    libraryDependencies ++= Seq(
      "org.scala-lang"                  % "scala-reflect"  % scalaVersion.value,
      "dev.zio"                       %%% "zio"            % zioVersion,
      "dev.zio"                       %%% "zio-streams"    % zioVersion,
      "dev.zio"                        %% "zio-query"      % zioQueryVersion,
      "dev.zio"                        %% "zio-test"       % zioVersion % Test,
      "io.suzaku"                     %%% "boopickle"      % boopickleVerison,
      "io.d11"                         %% "zhttp"          % zioHttpVersion,
      "com.softwaremill.sttp.client3" %%% "core"           % sttpVersion,
      "io.getquill"                    %% "quill-jdbc-zio" % quillVersion,
      "org.postgresql"                  % "postgresql"     % postgresVersion
    )
  )

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(ShoconPlugin)
  .in(file("examples"))
  .settings(
    name := "zio-app-examples",
    crossScalaVersions := supportedScalaVersions,
    publish / skip := true,
    libraryDependencies ++= Seq(
      "dev.zio"              %%% "zio"       % zioVersion,
      "dev.zio"               %% "zio-test"  % zioVersion % Test,
      "io.d11"                %% "zhttp"     % zioHttpVersion,
      "io.github.kitlangton" %%% "zio-magic" % zioMagicVersion
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core"                   % sttpVersion,
      "com.softwaremill.sttp.client3"  %% "httpclient-backend-zio" % sttpVersion
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSLinkerConfig ~= {
      _.withSourceMap(false)
    },
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo"         %%% "laminar"              % laminarVersion,
      "io.github.cquiroz" %%% "scala-java-time"      % scalaJavaTimeVersion,
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % scalaJavaTimeVersion,
      "org.akka-js"       %%% "shocon"               % shoconVersion
    ),
    (Compile / compile) := (Compile / compile).dependsOn(shoconConcat).value,
    shoconConcatFile := {
      autoImport.buildEnv.value match {
        case BuildEnv.Production =>
          (Compile / packageBin / artifactPath).value / "scala-2.13/zio-app-examples-opt/shocon.conf"
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
  .dependsOn(core)

def welcomeMessage = onLoadMessage := {
  import scala.Console

  def header(text: String): String = s"${Console.RED}$text${Console.RESET}"

  def item(text: String): String = s"${Console.GREEN}> ${Console.CYAN}$text${Console.RESET}"

  def subItem(text: String): String = s"  ${Console.YELLOW}> ${Console.CYAN}$text${Console.RESET}"

  s"""|${header(" ________ ___")}
      |${header("|__  /_ _/ _ \\")}
      |${header("  / / | | | | |")}
      |${header(" / /_ | | |_| |")}
      |${header(s"/____|___\\___/   ${version.value}")}
      |
      |Useful sbt tasks:
      |${item("build")} - Prepares sources, compiles and runs tests
      |${item("prepare")} - Prepares sources by applying both scalafix and scalafmt
      |${item("fix")} - Fixes sources files using scalafix
      |${item("fmt")} - Formats source files using scalafmt
      |${item("~compileJVM")} - Compiles all JVM modules (file-watch enabled)
      |${item("testJVM")} - Runs all JVM tests
      |${item("testJS")} - Runs all ScalaJS tests
      |${item("testOnly *.YourSpec -- -t \"YourLabel\"")} - Only runs tests with matching term e.g.
      |${subItem("coreTestsJVM/testOnly *.ZIOSpec -- -t \"happy-path\"")}
      |${item("docs/docusaurusCreateSite")} - Generates the ZIO microsite
      """.stripMargin
}
