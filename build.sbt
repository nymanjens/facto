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

lazy val optimizeForRelease = settingKey[Boolean]("If true, this is a release build")

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
    // Add custom setting
    optimizeForRelease := false,
    // Basic settings
    name := "client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    scalacOptions ++= (if (optimizeForRelease.value) Seq("-Xelide-below", "WARNING") else Seq()),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework"),
    // Execute the tests in browser-like environment
    requiresDOM in Test := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    emitSourceMaps in fastOptJS := false,
    // scalajs-bundler NPM packages
    npmDependencies in Compile ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    webpackConfigFile := Some(
      baseDirectory.value / (if (optimizeForRelease.value) "../webpack.prod.js" else "../webpack.dev.js")),
    webpackConfigFile in Test := None,
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    webpackBundlingMode in Test := BundlingMode.LibraryAndApplication()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

lazy val webworkerClient: Project = (project in file("app/js/webworker"))
  .settings(
    // Add custom setting
    optimizeForRelease := false,
    // Basic settings
    name := "webworker-client",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    scalacOptions ++= (if (optimizeForRelease.value) Seq("-Xelide-below", "WARNING") else Seq()),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    emitSourceMaps in fastOptJS := false,
    // scalajs-bundler NPM packages
    npmDependencies in Compile ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    webpackConfigFile := Some(
      baseDirectory.value / (if (optimizeForRelease.value) "../webpack.prod.js" else "../webpack.dev.js")),
    // Enable faster builds when developing
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(sharedJsCopy, jsShared)

lazy val manualTests: Project = (project in file("app/js/manualtests"))
  .settings(
    // Add custom setting
    optimizeForRelease := false,
    // Basic settings
    name := "manual-tests",
    version := BuildSettings.version,
    scalaVersion := BuildSettings.versions.scala,
    scalacOptions ++= BuildSettings.scalacOptions,
    scalacOptions ++= (if (optimizeForRelease.value) Seq("-Xelide-below", "WARNING") else Seq()),
    libraryDependencies ++= BuildSettings.scalajsDependencies.value,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    // Fix for bug that produces a huge amount of warnings (https://github.com/webpack/webpack/issues/4518).
    // Unfortunately, this means no source maps :-/
    emitSourceMaps in fastOptJS := false,
    // scalajs-bundler NPM packages
    npmDependencies in Compile ++= BuildSettings.npmDependencies(baseDirectory.value / "../../.."),
    // Custom webpack config
    webpackConfigFile := Some(
      baseDirectory.value / (if (optimizeForRelease.value) "../webpack.prod.js" else "../webpack.dev.js")),
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
    commands += ReleaseCmd,
    javaOptions := Seq("-Dconfig.file=conf/application.conf"),
    javaOptions in Test := Seq("-Dconfig.resource=test-application.conf"),
    // connect to the client project
    scalaJSProjects := clientProjects,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, digest, gzip),
    // Expose as sbt-web assets some files retrieved from the NPM packages of the `client` project
    // @formatter:off
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "jquery").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "bootstrap").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "metismenu").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "font-awesome").*** }.value,
    npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "startbootstrap-sb-admin-2").*** }.value,
    // @formatter:on
    // compress CSS
    LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .disablePlugins(PlayFilters) // Don't use the default filters
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clientProjects.map(projectToRef): _*)
  .dependsOn(sharedJvmCopy)

// Command for building a release
lazy val ReleaseCmd = Command.command("releaseOptimized") { state =>
  "set optimizeForRelease in client := true" ::
    "set optimizeForRelease in webworkerClient := true" ::
    "set optimizeForRelease in manualTests := true" ::
    "server/dist" ::
    "set optimizeForRelease in client := false" ::
    "set optimizeForRelease in webworkerClient := false" ::
    "set optimizeForRelease in manualTests := false" ::
    state
}

// loads the Play server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
