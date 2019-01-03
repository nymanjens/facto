package hydro.jsfacades

import org.scalajs.dom.raw.KeyboardEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Mousetrap {

  def bind(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit =
    RegularMousetrap.bind(key, callback)
  def bindGlobal(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit =
    GlobalMousetrap.bindGlobal(key, callback)

  @JSImport("mousetrap", JSImport.Namespace)
  @js.native
  object RegularMousetrap extends js.Object {
    def bind(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit = js.native
  }

  @JSImport("global-mousetrap", JSImport.Namespace)
  @js.native
  object GlobalMousetrap extends js.Object {
    def bindGlobal(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit = js.native
  }
}
