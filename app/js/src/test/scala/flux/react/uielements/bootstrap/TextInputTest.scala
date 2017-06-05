package flux.react.uielements.bootstrap

import common.LoggingUtils.logExceptions
import common.testing.{ReactTestWrapper, TestModule}
import flux.react.uielements
import flux.react.uielements.InputBase
import flux.react.uielements.InputBase.Listener
import japgolly.scalajs.react.{ReactElement, _}
import japgolly.scalajs.react.test.ReactTestUtils
import utest._

import scala.collection.mutable
import scala2js.Converters._

object TextInputTest extends TestSuite {
  implicit val fake18n = new TestModule().fakeI18n
  val testRef = TextInput.ref("testRef")

  override def tests = TestSuite {
    "Starts with default value" - {
      val tester = createComponent(defaultValue = "startvalue")

      tester.valueProxy.valueOrDefault ==> "startvalue"
    }

    "Does not show error message if valid value" - {
      val tester = createComponent(defaultValue = "valid value", required = true, showErrorMessage = true)

      tester.hasError ==> false
    }

    "Does not show error message if not required" - {
      val tester = createComponent(defaultValue = "", required = false, showErrorMessage = true)

      tester.hasError ==> false
    }

    "Shows error message" - {
      val tester = createComponent(defaultValue = "", required = true, showErrorMessage = true)

      tester.hasError ==> true
    }

    "Shows error message after value change" - {
      val tester = createComponent(defaultValue = "valid value", required = true, showErrorMessage = true)
      tester.valueProxy.setValue("")
      tester.hasError ==> true
    }
  }

  private def createComponent(defaultValue: String = "",
                              required: Boolean = false,
                              showErrorMessage: Boolean = false): ComponentTester = {
    new ComponentTester(ReactTestComponent {
      uielements.bootstrap.TextInput(
        ref = testRef,
        label = "label",
        required = required,
        defaultValue = defaultValue,
        showErrorMessage = showErrorMessage,
        focusOnMount = true
      )
    })
  }

  object ReactTestComponent {
    private val component = ReactComponentB[Props](getClass.getSimpleName)
      .renderBackend[Backend]
      .build

    // **************** public API **************** //
    def apply(render: => ReactElement): ComponentU = {
      component(Props(() => render))
    }

    // **************** public inner types **************** //
    type ComponentU = ReactComponentU[_, _, ReactTestComponent.Backend, _ <: TopNode]
    final class Backend(scope: BackendScope[Props, _]) {
      def render(props: Props): ReactElement = logExceptions {
        props.render()
      }

      def $ : BackendScope[_, _] = scope
    }

    // **************** private inner types **************** //
    private case class Props(render: () => ReactElement)
  }

  private final class ComponentTester(unrenderedComponent: ReactTestComponent.ComponentU) {
    private val renderedComponent = ReactTestUtils.renderIntoDocument(unrenderedComponent)
    private val wrappedComponent = new ReactTestWrapper(renderedComponent)

    def valueProxy: InputBase.Proxy[String] = {
      testRef(renderedComponent.backend.$)
    }

    def inputName: String = {
      wrappedComponent.child(tagName = "input").attribute("name")
    }

    def hasError: Boolean = {
      wrappedComponent.classes contains "has-error"
    }
  }
}
