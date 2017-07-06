package flux.react

import flux.FactoAppModule
import org.scalajs.dom

import scala.async.Async.{async, await}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("FactoApp")
object FactoApp extends js.JSApp {

  @JSExport
  def main(): Unit = async {
    println("  Application starting")
    // send log messages also to the server
    //log.enableServerLogging("/logging")
    //log.info("This message goes to server as well")

    // create stylesheet
    //GlobalStyles.addToDocument()

    val initialDataResponseFuture = api.Module.scalaJsApiClient.getInitialData()
    val remoteDatabaseProxyFuture = models.access.Module.remoteDatabaseProxy

    implicit val initialDataResponse = await(initialDataResponseFuture)
    implicit val remoteDatabaseProxy = await(remoteDatabaseProxyFuture)

    implicit val globalModule = new FactoAppModule()

    // tell React to render the router in the document body
    globalModule.router().renderIntoDOM(dom.document.getElementById("root"))
  }
}
