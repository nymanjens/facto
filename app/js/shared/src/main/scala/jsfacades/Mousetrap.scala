package jsfacades

import org.scalajs.dom.raw.KeyboardEvent
import app.scala2js.Converters._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("global-mousetrap", JSImport.Namespace)
@js.native
object Mousetrap extends js.Object {

  def bindGlobal(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit = js.native
}
