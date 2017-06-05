package common.testing

import common.LoggingUtils.logExceptions
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactComponentU, ReactElement, TopNode}

object TestComponentWithBackendScope {
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** public API **************** //
  def apply(render: => ReactElement): ComponentU = {
    component(Props(() => render))
  }

  // **************** public inner types **************** //
  type ComponentU = ReactComponentU[_, _, TestComponentWithBackendScope.Backend, _ <: TopNode]
  final class Backend(scope: BackendScope[Props, _]) {
    def render(props: Props): ReactElement = logExceptions {
      props.render()
    }

    def $ : BackendScope[_, _] = scope
  }

  // **************** private inner types **************** //
  private case class Props(render: () => ReactElement)
}
