package flux.react.app.transactiongroupform

import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.ReactComponentC.ReqProps
import flux.react.uielements.bootstrap.TextInput
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq

object DefaultFromReference {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](ConnectionState.ConnectedToDefault)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(inputElementRef: TextInput.Reference,
            defaultValueRef: TextInput.Reference
           )(inputElementFactory: InputElementExtraProps => ReactElement): ReactElement = {
    component(Props(inputElementRef, inputElementFactory, defaultValueRef))
  }

  // **************** Public inner types ****************//
  case class InputElementExtraProps(ref: TextInput.Reference, inputClasses: Seq[String])

  // **************** Private inner types ****************//
  private type State = ConnectionState

  private case class Props(inputElementRef: TextInput.Reference,
                           inputElementFactory: InputElementExtraProps => ReactElement,
                           defaultValueRef: TextInput.Reference)

  private final class Backend($: BackendScope[Props, State]) {

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      val inputClasses = if (state == ConnectionState.ConnectedToDefault) Seq("bound-until-change") else Seq()
      props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses))
    }
  }

  sealed trait ConnectionState
  object ConnectionState {
    object ConnectedToDefault extends ConnectionState
    object Disconnected extends ConnectionState
  }
}
