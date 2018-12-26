package app.api

final class Module {

  implicit lazy val scalaJsApiClient: ScalaJsApiClient = new ScalaJsApiClient.Impl
}
