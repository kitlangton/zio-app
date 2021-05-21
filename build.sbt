import xerial.sbt.Sonatype.autoImport.sonatypeCredentialHost

inThisBuild(
  List(
    name := "zio-app",
    normalizedName := "zio-app",
    organization := "io.github.kitlangton",
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.13.6"),
    organization := "io.github.kitlangton",
    homepage := Some(url("https://github.com/kitlangton/zio-app")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
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

lazy val Scala213               = "2.13.6"
lazy val scala3                 = "3.0.0"
lazy val supportedScalaVersions = List(Scala213)

val zioHttpVersion  = "1.0.0.0-RC16"
val zioVersion      = "1.0.8"
val laminarVersion  = "0.13.0"
val zioMagicVersion = "0.3.2"

lazy val root = (project in file("."))
  .aggregate(cli, core.jvm, core.js)
  .settings(
    name := "zio-app",
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val cli = (project in file("cli"))
  .enablePlugins(NativeImagePlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "zio-app-cli",
    publish / skip := true,
    nativeImageOptions ++= List(
      "--no-fallback",
      "--allow-incomplete-classpath",
      "--initialize-at-build-time=org.eclipse.jgit.ignore.internal.PathMatcher",
      "-H:ReflectionConfigurationFiles=../../src/main/resources/reflection-config.json",
      "--report-unsupported-elements-at-runtime"
    ),
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio"         % zioVersion,
      "dev.zio"              %% "zio-process" % "0.3.0",
      "dev.zio"              %% "zio-streams" % zioVersion,
      "dev.zio"              %% "zio-nio"     % "1.0.0-RC10",
      "dev.zio"              %% "zio-test"    % zioVersion % Test,
      "io.github.kitlangton" %% "zio-magic"   % zioMagicVersion,
      "org.jline"             % "jline"       % "3.19.0",
      "log4j"                 % "log4j"       % "1.2.15"
    ),
    Compile / mainClass := Some("zio.app.Main"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(
    name := "zio-app",
    ThisBuild / scalaVersion := Scala213,
    crossScalaVersions := supportedScalaVersions,
    publish / skip := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      "org.scala-lang"                  % "scala-reflect" % scalaVersion.value,
      "dev.zio"                        %% "zio"           % zioVersion,
      "dev.zio"                        %% "zio-test"      % zioVersion % Test,
      "io.suzaku"                     %%% "boopickle"     % "1.3.2",
      "io.d11"                         %% "zhttp"         % zioHttpVersion,
      "com.softwaremill.sttp.client3" %%% "core"          % "3.3.3"
    )
  )

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .in(file("examples"))
  .settings(
    name := "zio-app-examples",
    crossScalaVersions := supportedScalaVersions,
    publish / skip := true,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"      % zioVersion,
      "dev.zio"  %% "zio-test" % zioVersion % Test,
      "io.d11"   %% "zhttp"    % zioHttpVersion
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % laminarVersion
    )
  )
  .dependsOn(core)
