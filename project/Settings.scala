import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {

  /** The name of your application */
  val name = "facto"

  /** The version of your application */
  val version = "3.0"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    // "-Xfatal-warnings",
    "-Xlint:-unused,_",
    "-Ywarn-unused:-imports"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.12.2" // Must be the same as in .travis.yml
    val play = "2.6.0-RC2" // Must be the same as the Play sbt-plugin in plugins.sbt

    val uTest = "0.4.7"
    val scalajsReact = "1.0.0"
    val diode = "1.1.0"
    val jQuery = "2.2.4"
    val bootstrap = "3.3.6"
  }

  private object webjarDeps {
    val jQuery = "org.webjars" % "jquery" % versions.jQuery
    val bootstrap = "org.webjars" % "bootstrap" % versions.bootstrap

    val bootstrapTagsinput = "org.webjars.bower" % "bootstrap-tagsinput" % "0.8.0"
    val chartJs = "org.webjars" % "chartjs" % "2.1.3"
    val fontAwesome = "org.webjars" % "font-awesome" % "4.6.2"
    val laddaBootstrap = "org.webjars.bower" % "ladda-bootstrap" % "0.1.0"
    val log4Javascript = "org.webjars" % "log4javascript" % "1.4.10"
    val lokijs = "org.webjars.bower" % "lokijs" % "1.4.2"
    val metisMenu = "org.webjars" % "metisMenu" % "1.1.3"
    val mousetrap = "org.webjars" % "mousetrap" % "1.5.3-1"
    val react = "org.webjars.bower" % "react" % "15.3.2"
    val sha1 = "org.webjars.bower" % "SHA-1" % "0.1.1"
    val typeaheadJs = "org.webjars" % "typeaheadjs" % "0.11.1"
    val webjarsPlay = "org.webjars" %% "webjars-play" % "2.6.0-M1"
  }

  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedDependencies = Def.setting(
    Seq(
      "org.scala-lang.modules" %% "scala-async" % "0.9.6",
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "me.chrons" %%% "boopickle" % "1.2.5"
    ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(
    Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.0",
      "com.lihaoyi" %% "utest" % versions.uTest % Test,
      "com.typesafe.play" %% "play-jdbc" % versions.play,
      "com.typesafe.play" %% "play-cache" % versions.play,
      "com.typesafe.play" %% "play-ws" % versions.play,
      "com.typesafe.play" %% "play-specs2" % versions.play % Test,
      "org.yaml" % "snakeyaml" % "1.14",
      "com.typesafe.slick" %% "slick" % "3.2.0",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
      "commons-lang" % "commons-lang" % "2.6",
      "mysql" % "mysql-connector-java" % "5.1.36",
      "com.h2database" % "h2" % "1.4.195" % Test,
      "org.xerial" % "sqlite-jdbc" % "3.8.11.2",
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "net.jcip" % "jcip-annotations" % "1.0",
      webjarDeps.bootstrap,
      webjarDeps.webjarsPlay,
      webjarDeps.fontAwesome,
      webjarDeps.bootstrapTagsinput,
      webjarDeps.laddaBootstrap,
      webjarDeps.typeaheadJs
    ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(
    Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % versions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % versions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "test" % versions.scalajsReact % Test,
      "com.github.japgolly.scalacss" %%% "ext-react" % "0.5.3",
      "me.chrons" %%% "diode" % versions.diode,
      "me.chrons" %%% "diode-react" % versions.diode,
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "org.scala-js" %%% "scalajs-java-time" % "0.2.0",
      "com.softwaremill.macwire" %% "macros" % "2.2.5",
      "com.lihaoyi" %%% "utest" % versions.uTest % Test
    ))

  private object files {
    val jQuery = s"${versions.jQuery}/jquery.min.js"
    val bootstrap = s"${versions.bootstrap}/js/bootstrap.min.js"
    val reactWithAddons = "react-with-addons.js"
    val reactDom = "react-dom.js"
  }

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(
    Seq(
      webjarDeps.react / files.reactWithAddons minified "react-with-addons.min.js" commonJSName "React",
      webjarDeps.react / files.reactDom minified "react-dom.min.js" dependsOn files.reactWithAddons commonJSName "ReactDOM",
      webjarDeps.react % Test / "react-dom-server.js" minified "react-dom-server.min.js" dependsOn files.reactDom commonJSName "ReactDOMServer",
      webjarDeps.jQuery / files.jQuery,
      webjarDeps.bootstrap / files.bootstrap dependsOn files.jQuery,
      webjarDeps.metisMenu / "metisMenu.min.js" dependsOn files.bootstrap,
      webjarDeps.mousetrap / "mousetrap.min.js",
      webjarDeps.bootstrapTagsinput / "bootstrap-tagsinput.min.js" dependsOn files.bootstrap,
      webjarDeps.typeaheadJs / "typeahead.bundle.min.js" dependsOn files.bootstrap,
      webjarDeps.sha1 / "sha1.js",
      webjarDeps.laddaBootstrap / "spin.min.js",
      webjarDeps.laddaBootstrap / "ladda.min.js",
      webjarDeps.chartJs / "Chart.min.js",
      webjarDeps.log4Javascript / "js/log4javascript.js",
      webjarDeps.lokijs / "lokijs.min.js",
      webjarDeps.lokijs / "loki-indexed-adapter.min.js"
    ))
}
