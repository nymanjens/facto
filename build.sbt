import sbt.Keys._
import sbt.Project.projectToRef

// Treat eviction issues as warning
ThisBuild / evictionErrorLevel := Level.Warn

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("app/shared"))
  .settings(
    scalaVersion := BuildSettings.versions.scala,
    libraryDependencies ++= BuildSettings.sharedDependencies.value
  )

lazy val sharedJvmCopy = shared.jvm.settings(name := "sharedJVM")

lazy val sharedJsCopy = shared.js.settings(name := "sharedJS")

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
    // Basic settings
    name := "client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    fullOptJS / scalacOptions ++= Seq("-Xelide-below", "WARNING"),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework"),
    // Execute the tests in browser-like environment
    Test / requiresDOM := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    fastOptJS / emitSourceMaps := false,
    // scalajs-bundler NPM packages
    Compile / npmDependencies ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    Test / webpackConfigFile := None,
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.dev.js"),
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.prod.js"),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    Test / webpackBundlingMode := BundlingMode.LibraryAndApplication()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

lazy val webworkerClient: Project = (project in file("app/js/webworker"))
  .settings(
    // Basic settings
    name := "webworker-client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    fullOptJS / scalacOptions ++= Seq("-Xelide-below", "WARNING"),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    fastOptJS / emitSourceMaps := false,
    // scalajs-bundler NPM packages
    Compile / npmDependencies ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    Test / webpackConfigFile := None,
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.dev.js"),
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.prod.js"),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

lazy val manualTests: Project = (project in file("app/js/manualtests"))
  .settings(
    // Basic settings
    name := "manual-tests",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    fullOptJS / scalacOptions ++= Seq("-Xelide-below", "WARNING"),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    fastOptJS / emitSourceMaps := false,
    // scalajs-bundler NPM packages
    Compile / npmDependencies ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    Test / webpackConfigFile := None,
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.dev.js"),
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / "../webpack.prod.js"),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

// Client projects
lazy val clientProjects = Seq(client, webworkerClient, manualTests)

lazy val server = (project in file("app/jvm"))
  .settings(
    name := "server",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    libraryDependencies ++= BuildSettings.jvmDependencies.value,
    libraryDependencies += guice,
    javaOptions := Seq("-Dconfig.file=conf/application.conf"),
    Test / javaOptions := Seq("-Dconfig.resource=test-application.conf"),
    // connect to the client project
    scalaJSProjects := clientProjects,
    Assets / pipelineStages := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, digest, gzip),
    // Expose as sbt-web assets some files retrieved from the NPM packages of the `client` project
    // @formatter:off
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "jquery").allPaths }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "bootstrap").allPaths }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "metismenu").allPaths }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "font-awesome").allPaths }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "startbootstrap-sb-admin-2").allPaths }.value,
    // @formatter:on
    // compress CSS
    Assets / LessKeys.compress := true
  )
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .disablePlugins(PlayFilters) // Don't use the default filters
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clientProjects.map(projectToRef): _*)
  .dependsOn(sharedJvmCopy)

// loads the Play server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
