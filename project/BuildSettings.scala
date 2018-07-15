import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object BuildSettings {

  /** The name of your application */
  val name = "facto"

  /** The version of your application */
  val version = "3.0"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs
    "-encoding",
    "UTF-8",
    "-feature", // Emit warning and location for usages of features that should be imported explicitly
    "-unchecked", // Enable additional warnings where generated code depends on assumptions
    "-Xfatal-warnings", // Make warnings behave like errors
    "-Xfuture", // Warn of changes to future major Scala version
    "-Xlint:-unused,_",
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ywarn-dead-code",
    "-Ywarn-unused:-imports"
    // "-Ywarn-value-discard" // Warning for value discard bugs (see
    // http://underscore.io/blog/posts/2016/11/24/value-discard.html)
    // Disabled because some cases (e.g. Guice modules) actually
    // benefit from this feature.
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.12.2" // Must be the same as in .travis.yml
    val play = "2.6.15" // Must be the same as the Play sbt-plugin in plugins.sbt

    val uTest = "0.4.7"
    val scalajsReact = "1.2.1"
    val diode = "1.1.0"
    val jQuery = "2.2.4"
    val bootstrap = "3.3.6"
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
      "com.vmunier" %% "scalajs-scripts" % "1.1.2",
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
      "net.jcip" % "jcip-annotations" % "1.0"
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
      "com.lihaoyi" %%% "utest" % versions.uTest % Test
    ))

  def npmDependencies(projectRootDirectory: File): Seq[(String, String)] = Seq(
    // For assets only
    "jquery" -> "1.11.1",
    "bootstrap" -> "3.3.6",
    "metismenu" -> "1.1.3",
    "font-awesome" -> "4.6.3",
    "startbootstrap-sb-admin-2" -> "1.0.7",
    // Used in ScalaJS code
    "lokijs" -> "1.4.2",
    "react" -> "16.2.0",
    "react-dom" -> "16.2.0",
    "react-tag-input" -> "4.7.2",
    "react-autosuggest" -> "9.3.2",
    "global-mousetrap" -> s"file:${projectRootDirectory / "app/js/shared/src/main/npm-packages/global-mousetrap"}"
  )
}
