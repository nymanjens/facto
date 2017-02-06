package spatutorial.client

import flux.FactoAppModule
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  @JSExport
  def main(): Unit = {
    println("  Application starting")
    // send log messages also to the server
    //    log.enableServerLogging("/logging")
    //    log.info("This message goes to server as well")

    // create stylesheet
    //    GlobalStyles.addToDocument()

    api.Module.scalaJsApiClient.getInitialData() map { implicit response =>
      implicit val globalModule = new FactoAppModule()

      // create the router
      val router = globalModule.routerFactory.createRouter()

      // tell React to render the router in the document body
      ReactDOM.render(router(), dom.document.getElementById("root"))
    }

  }
}
