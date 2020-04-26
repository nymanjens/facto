package hydro.jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("clipboard-polyfill", JSImport.Namespace)
@js.native
object ClipboardPolyfill extends js.Object {
  def write(dataTransfer: DT): Unit = js.native
  def writeText(text: String): Unit = js.native

  @js.native
  class DT extends js.Object {
    def setData(tpe: String, data: String): Unit = js.native
  }
}
