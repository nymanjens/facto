package hydro.jsfacades

import hydro.common.LoggingUtils
import hydro.common.LoggingUtils.logExceptions
import japgolly.scalajs.react.Children
import japgolly.scalajs.react.JsComponent
import org.scalajs.dom
import org.scalajs.dom.raw.KeyboardEvent

import scala.scalajs.js
import app.scala2js.Converters._

import scala.scalajs.js.annotation.JSImport

object ReactContentEditable {

  // **************** API ****************//
  def apply(html: String, onChange: String => Unit, onKeyDown: KeyboardEvent => Unit = _ => {}) = {
    val component = JsComponent[js.Object, Children.None, Null](RawComponent)
    component(
      Props(
        html = html,
        onChange = evt =>
          logExceptions {
            onChange(evt.target.value.asInstanceOf[String])
        },
        onKeyDown = onKeyDown).toJsObject)
  }

  // **************** Private inner types ****************//
  @JSImport("react-contenteditable", JSImport.Namespace)
  @js.native
  private object RawComponent extends js.Object

  private case class Props(html: String,
                           onChange: js.Function1[js.Dynamic, Unit],
                           onKeyDown: js.Function1[KeyboardEvent, Unit]) {
    def toJsObject: js.Object =
      js.Dynamic.literal(
        html = html,
        disabled = false,
        onChange = onChange,
        onKeyDown = onKeyDown
      )
  }
}
