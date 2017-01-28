package spatutorial.client

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import japgolly.scalajs.react.ReactDOM
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import spatutorial.client.spacomponents.GlobalStyles
import spatutorial.client.logger._
import spatutorial.client.modules._
import spatutorial.client.services.SPACircuit

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  // Define the locations (pages) used in this application
  sealed trait Loc

  case object DashboardLoc extends Loc

  case object TodoLoc extends Loc

  // configure the router
  def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Loc].buildConfig { dsl =>
      import dsl._

      val todoWrapper = SPACircuit.connect(_.todos)
      // wrap/connect components to the circuit
      (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(_.motd)(proxy => Dashboard(ctl, proxy)))
        | staticRoute("#todo", TodoLoc) ~> renderR(ctl => todoWrapper(Todo(_)))
        ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
    }.renderWith(layout)
  }

  val todoCountWrapper = SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)
  // base layout for all pages
  def layout(c: RouterCtl[Loc], r: Resolution[Loc])(implicit reactAppModule: flux.react.app.Module) = {
    <.div(
      reactAppModule.everything(20)
    )
  }

  @JSExport
  def main(): Unit = {
    log.warn("Application starting")
    // send log messages also to the server
    //    log.enableServerLogging("/logging")
    //    log.info("This message goes to server as well")

    // create stylesheet
    //    GlobalStyles.addToDocument()

    api.Module.scalaJsApiClient.getInitialData() map { implicit response =>
      implicit val reactAppModule = new flux.react.app.Module

      // create the router
      val router = Router(BaseUrl.until_#, routerConfig)

      // tell React to render the router in the document body
      ReactDOM.render(router(), dom.document.getElementById("root"))
    }

  }
}
