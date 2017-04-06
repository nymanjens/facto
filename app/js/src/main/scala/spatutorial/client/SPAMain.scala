package spatutorial.client

import scala.async.Async.{async, await}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import flux.FactoAppModule
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  @JSExport
  def main(): Unit = async {
    println("  Application starting")
    // send log messages also to the server
    //    log.enableServerLogging("/logging")
    //    log.info("This message goes to server as well")

    // create stylesheet
    //    GlobalStyles.addToDocument()

    val initialDataResponseFuture = api.Module.scalaJsApiClient.getInitialData()
    val remoteDatabaseProxyFuture = models.access.Module.remoteDatabaseProxy

    implicit val initialDataResponse = await(initialDataResponseFuture)
    implicit val remoteDatabaseProxy = await(remoteDatabaseProxyFuture)

    implicit val globalModule = new FactoAppModule()

    // create the router
    val router = globalModule.routerFactory.createRouter()

    // tell React to render the router in the document body
    ReactDOM.render(router(), dom.document.getElementById("root"))
  }
}
