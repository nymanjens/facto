package flux.react.uielements

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import common.LoggingUtils
import common.LoggingUtils.LogExceptionsCallback
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class WaitForFuture[V] {
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(input = None))
    .renderPS((_, props, state) =>
      state.input match {
        case Some(input) => props.inputToElement(input)
        case None => <.span("loading...")
    })
    .componentWillMount($ =>
      LogExceptionsCallback {
        $.props.futureInput.map(input => $.modState(_.copy(input = Some(input))).runNow())
    })
    .build

  // **************** API ****************//
  def apply(futureInput: Future[V])(inputToElement: V => VdomElement): VdomElement = {
    component.apply(Props(futureInput = futureInput, inputToElement = inputToElement))
  }

  // **************** Private inner types ****************//
  private case class Props(futureInput: Future[V], inputToElement: V => VdomElement)
  private case class State(input: Option[V])
}
