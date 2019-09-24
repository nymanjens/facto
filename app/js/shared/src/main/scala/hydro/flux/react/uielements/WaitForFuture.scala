package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.Failure
import scala.util.Success

final class WaitForFuture[V] {
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(input = None))
    .renderPS((_, props, state) =>
      state.input match {
        case Some(input) => props.inputToElement(input)
        case None        => props.waitingElement
    })
    .componentWillMount($ =>
      LogExceptionsCallback {
        $.props.futureInput.map(input => $.modState(_.copy(input = Some(input))).runNow())
    })
    .build

  // **************** API ****************//
  def apply(futureInput: Future[V], waitingElement: VdomElement = null)(inputToElement: V => VdomElement)(
      implicit i18n: I18n): VdomElement = {
    futureInput.value match {
      case Some(Success(value)) => inputToElement(value)
      case Some(Failure(_))     => waitingElement
      case None =>
        component.apply(
          Props(
            futureInput = futureInput,
            inputToElement = inputToElement,
            waitingElement = Option(waitingElement) getOrElse defaultWaitingElement))
    }
  }

  private def defaultWaitingElement(implicit i18n: I18n): VdomElement =
    <.div(^.style := js.Dictionary("padding" -> "200px 0  500px 60px"), s"${i18n("app.loading")}...")

  // **************** Private inner types ****************//
  private case class Props(
      futureInput: Future[V],
      inputToElement: V => VdomElement,
      waitingElement: VdomElement,
  )
  private case class State(input: Option[V])
}
