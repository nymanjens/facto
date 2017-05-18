package flux.react.router

import japgolly.scalajs.react.extra.router._

final class Module(implicit reactAppModule: flux.react.app.Module) {

  import com.softwaremill.macwire._

  implicit lazy val router: Router[Page] = wire[RouterFactory].createRouter()
}
