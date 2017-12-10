package flux.react

import common.LoggingUtils.logExceptions
import flux.FactoAppModule
import org.scalajs.dom

import scala.async.Async.{async, await}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("FactoApp")
object FactoApp extends js.JSApp {

  @JSExport
  override def main(): Unit = async {
    println("  Application starting")
    // send log messages also to the server
    //log.enableServerLogging("/logging")
    //log.info("This message goes to server as well")

    // create stylesheet
    //GlobalStyles.addToDocument()

    val apiModule = new api.Module
    implicit val scalaJsApiClient = apiModule.scalaJsApiClient
    implicit val initialDataResponse = await(scalaJsApiClient.getInitialData())

    val modelsAccessModule = new models.access.Module(initialDataResponse.user)
    implicit val remoteDatabaseProxy = await(modelsAccessModule.remoteDatabaseProxy)

    implicit val globalModule = new FactoAppModule()

    // tell React to render the router in the document body
    logExceptions {
      globalModule.router().renderIntoDOM(dom.document.getElementById("root"))
    }
  }
}
