// // repository for Typesafe plugins
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

// Tell the compiler to ignore version mismatches in scala-xml
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.20") // Must be the same as BuildSettings.versions.play
// addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.3.1")

// scala.js plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

// Web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.13.0")

// Other
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")
// addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
