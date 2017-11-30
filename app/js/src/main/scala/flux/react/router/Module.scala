package flux.react.router

import common.I18n
import flux.action.Dispatcher
import japgolly.scalajs.react.extra.router._

final class Module(implicit reactAppModule: flux.react.app.Module, dispatcher: Dispatcher, i18n: I18n) {

  import com.softwaremill.macwire._

  implicit lazy val router: Router[Page] = wire[RouterFactory].createRouter()
}
