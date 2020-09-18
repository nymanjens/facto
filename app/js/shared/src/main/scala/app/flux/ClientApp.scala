package app.flux

import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.logFailure
import org.scalajs.dom
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object ClientApp {

  def main(): Unit = async {
    console.log("  Application starting")

    // create stylesheet
    //GlobalStyles.addToDocument()

    setUpServiceWorker()

    val commonTimeModule = new hydro.common.time.Module
    implicit val clock = commonTimeModule.clock

    val apiModule = new app.api.Module
    implicit val scalaJsApiClient = apiModule.scalaJsApiClient
    implicit val initialDataResponse = await(logFailure(scalaJsApiClient.getInitialData()))

    logExceptions {
      implicit val globalModule = new ClientAppModule()

      // tell React to render the router in the document body
      globalModule.router().renderIntoDOM(dom.document.getElementById("root"))
    }
  }

  private def setUpServiceWorker(): Unit = logExceptions {
    val navigator = js.Dynamic.global.navigator
    if (!js.isUndefined(navigator.serviceWorker)) {
      navigator.serviceWorker
        .register("/serviceWorker.js")
        .`then`(
          (registration: Any) => {},
          (err: Any) => println(s"  Installation of service worker failed: ${err}"),
        )
    }
  }
}
