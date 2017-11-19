package flux.react.router

import common.I18n
import japgolly.scalajs.react.extra.router._

final class Module(implicit reactAppModule: flux.react.app.Module, i18n: I18n) {

  import com.softwaremill.macwire._

  implicit lazy val router: Router[Page] = wire[RouterFactory].createRouter()
}
