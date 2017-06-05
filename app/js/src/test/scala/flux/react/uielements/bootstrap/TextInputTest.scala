package flux.react.uielements.bootstrap

import common.LoggingUtils.logExceptions
import common.testing.{ReactTestWrapper, TestComponentWithBackendScope, TestModule}
import flux.react.uielements
import flux.react.uielements.InputBase
import flux.react.uielements.InputBase.Listener
import japgolly.scalajs.react.{VdomElement, _}
import japgolly.scalajs.react.test.ReactTestUtils
import utest._

import scala.collection.mutable
import scala2js.Converters._

object TextInputTest extends TestSuite {
  implicit private val fake18n = new TestModule().fakeI18n
  private val testRef = TextInput.ref("testRef")

  override def tests = TestSuite {
    "Starts with default value" - {
      val tester = createTestComponent(defaultValue = "startvalue")

      tester.valueProxy.valueOrDefault ==> "startvalue"
    }

    "Does not show error message if valid value" - {
      val tester = createTestComponent(defaultValue = "valid value", required = true, showErrorMessage = true)

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
      val tester = createTestComponent(defaultValue = "valid value", required = true, showErrorMessage = true)
      tester.valueProxy.setValue("")
      tester.hasError ==> true
    }
  }

  private def createTestComponent(defaultValue: String = "",
                              required: Boolean = false,
                              showErrorMessage: Boolean = false): ComponentTester = {
    new ComponentTester(TestComponentWithBackendScope {
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

  private final class ComponentTester(unrenderedComponent: TestComponentWithBackendScope.ComponentU) {
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
