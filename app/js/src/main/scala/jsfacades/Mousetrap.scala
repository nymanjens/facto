package jsfacades

import org.scalajs.dom.raw.KeyboardEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala2js.Converters._

@JSGlobal("Mousetrap")
@js.native
object Mousetrap extends js.Object {

  def bindGlobal(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit = js.native
}
