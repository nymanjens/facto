package api

final class Module {

  import com.softwaremill.macwire._

  implicit lazy val scalaJsApiClient: ScalaJsApiClient = wire[ScalaJsApiClient.Impl]
}
