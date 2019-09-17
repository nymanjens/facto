package hydro.flux.react.uielements.input.bootstrap

import hydro.common.testing.ReactTestWrapper
import app.common.testing.TestModule
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.bootstrap
import japgolly.scalajs.react.vdom._
import utest._

object TextInputTest extends TestSuite {
  implicit private val fake18n = new TestModule().fakeI18n
  private val testRef = TextInput.ref()

  override def tests = TestSuite {
    "Starts with default value" - {
      val tester = createTestComponent(defaultValue = "startvalue")

      tester.valueProxy.valueOrDefault ==> "startvalue"
    }

    "Does not show error message if valid value" - {
      val tester =
        createTestComponent(defaultValue = "valid value", required = true, showErrorMessage = true)

      tester.hasError ==> false
    }

    "Does not show error message if not required" - {
      val tester = createTestComponent(defaultValue = "", required = false, showErrorMessage = true)

      tester.hasError ==> false
    }

    "Shows error message" - {
      val tester = createTestComponent(defaultValue = "", required = true, showErrorMessage = true)

      tester.hasError ==> true
    }

    "Shows error message after value change" - {
      val tester =
        createTestComponent(defaultValue = "valid value", required = true, showErrorMessage = true)
      tester.valueProxy.setValue("")
      tester.hasError ==> true
    }

    "Shows error message if additional validator indicates an invalid value" - {
      val tester = createTestComponent(defaultValue = "INVALID_VALUE", showErrorMessage = true)

      tester.hasError ==> true
    }

    "Input name is given name" - {
      val tester = createTestComponent()
      tester.inputName ==> "dummy-name"
    }
  }

  private def createTestComponent(
      defaultValue: String = "",
      required: Boolean = false,
      showErrorMessage: Boolean = false,
  ): ComponentTester = {
    new ComponentTester(
      bootstrap.TextInput(
        ref = testRef,
        name = "dummy-name",
        label = "label",
        required = required,
        defaultValue = defaultValue,
        showErrorMessage = showErrorMessage,
        additionalValidator = _ != "INVALID_VALUE",
        focusOnMount = true
      )
    )
  }

  private final class ComponentTester(unrenderedComponent: VdomElement) {
    private val wrappedComponent = ReactTestWrapper.renderComponent(unrenderedComponent)

    def valueProxy: InputBase.Proxy[String] = {
      testRef()
    }

    def inputName: String = {
      wrappedComponent.child(tagName = "input").attribute("name")
    }

    def hasError: Boolean = {
      wrappedComponent.classes contains "has-error"
    }
  }
}
