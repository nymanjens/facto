package hydro.jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object WebWorker extends js.Object {

  def addEventListener(`type`: String, f: js.Function): Unit = js.native

  def postMessage(data: js.Any): Unit = js.native
}
