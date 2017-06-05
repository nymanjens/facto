package common.testing

import japgolly.scalajs.react.component.Scala.MutableRef
import common.LoggingUtils.logExceptions
import japgolly.scalajs.react._

object TestComponentWithBackendScope {
  private val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** public API **************** //
  def apply(render: => VdomElement): ComponentU = {
    component(Props(() => render))
  }

  // **************** public inner types **************** //
  type ComponentU = ReactComponentU[_, _, TestComponentWithBackendScope.Backend, _]
  final class Backend(scope: BackendScope[Props, _]) {
    def render(props: Props): VdomElement = logExceptions {
      props.render()
    }

    def $ : BackendScope[_, _] = scope
  }

  // **************** private inner types **************** //
  private case class Props(render: () => VdomElement)
}
