package hydro.jsfacades

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement

/** Facade on top of bootbox.js */
object Bootbox {

  def alert(str: String): Unit = {
    RawJsBootbox.alert(str)
  }

  def prompt(title: String, value: String, animate: Boolean, selectValue: Boolean): Future[Option[String]] = {
    val resultPromise: Promise[Option[String]] = Promise()
    val callback: js.Function1[String, Unit] = response => {
      resultPromise.success(Option(response))
    }

    RawJsBootbox.prompt(
      js.Dynamic.literal(
        title = title,
        value = value,
        callback = callback,
        animate = animate,
      )
    )

    if (selectValue && value.nonEmpty) {
      val promptInput =
        dom.document.getElementsByClassName("bootbox-input-text").apply(0).asInstanceOf[HTMLInputElement]
      promptInput.setSelectionRange(0, promptInput.value.length)
    }

    resultPromise.future
  }

  // Using global instead of import because bootbox seems to rely on JQuery and bootstrap.js being
  // present in the global scope
  @JSGlobal("bootbox")
  @js.native
  object RawJsBootbox extends js.Object {
    def alert(str: String): Unit = js.native
    def prompt(parameters: js.Object): Unit = js.native
  }
}
