import sbt.Keys._
import sbt.Project.projectToRef

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("app/shared"))
  .settings(
    scalaVersion := BuildSettings.versions.scala,
    libraryDependencies ++= BuildSettings.sharedDependencies.value
  )

lazy val sharedJvmCopy = shared.jvm.settings(name := "sharedJVM")

lazy val sharedJsCopy = shared.js.settings(name := "sharedJS")

// use eliding to drop some debug code in the production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

lazy val jsShared: Project = (project in file("app/js/shared"))
  .settings(
    name := "jsShared",
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .enablePlugins(ScalaJSWeb)
  .dependsOn(sharedJsCopy)

lazy val client: Project = (project in file("app/js/client"))
  .settings(
    name := "client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework"),
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    emitSourceMaps in fastOptJS := false,
    // scalajs-bundler NPM packages
    npmDependencies in Compile ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

lazy val webworkerClient: Project = (project in file("app/js/webworker"))
  .settings(
    name := "webworker-client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    emitSourceMaps in fastOptJS := false,
    // scalajs-bundler NPM packages
    npmDependencies in Compile ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

// Client projects
lazy val clientProjects = Seq(client, webworkerClient)

lazy val server = (project in file("app/jvm"))
  .settings(
    name := "server",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    libraryDependencies ++= BuildSettings.jvmDependencies.value,
    libraryDependencies += guice,
    commands += ReleaseCmd,
    javaOptions := Seq("-Dconfig.file=conf/application.conf"),
    javaOptions in Test := Seq("-Dconfig.resource=test-application.conf"),
    // connect to the client project
    scalaJSProjects := clientProjects,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, digest, gzip),
    // Expose as sbt-web assets some files retrieved from the NPM packages of the `client` project
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "jquery").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "bootstrap").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "metismenu").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "font-awesome").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "startbootstrap-sb-admin-2").*** }.value,
    // compress CSS
    LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .disablePlugins(PlayFilters) // Don't use the default filters
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clientProjects.map(projectToRef): _*)
  .dependsOn(sharedJvmCopy)

// Command for building a release
lazy val ReleaseCmd = Command.command("release") { state =>
  "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
    "client/clean" ::
    "client/test" ::
    "webworkerClient/clean" ::
    "server/clean" ::
    "server/test" ::
    "server/dist" ::
    "set elideOptions in client := Seq()" ::
    state
}

// loads the Play server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
