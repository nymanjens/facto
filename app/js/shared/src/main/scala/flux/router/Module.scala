package flux.router

import common.I18n
import hydro.flux.action.Dispatcher
import japgolly.scalajs.react.extra.router._

final class Module(implicit reactAppModule: flux.react.app.Module, dispatcher: Dispatcher, i18n: I18n) {

  implicit lazy val router: Router[Page] = (new RouterFactory).createRouter()
}
