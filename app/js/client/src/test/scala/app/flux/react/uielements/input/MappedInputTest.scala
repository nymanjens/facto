package app.flux.react.uielements.input

import java.time.Month.APRIL
import java.time.Month.MAY

import hydro.common.testing.ReactTestWrapper
import app.common.testing.TestModule
import hydro.common.time.LocalDateTime
import hydro.common.time.LocalDateTimes.createDateTime
import hydro.flux.react.uielements.input.bootstrap.TextInput
import japgolly.scalajs.react.vdom.VdomElement
import utest._

object MappedInputTest extends TestSuite {
  implicit private val fake18n = new TestModule().fakeI18n
  private val dateMappedInput = MappedInput.forTypes[String, LocalDateTime]

  private val defaultDate = createDateTime(2017, APRIL, 3)

  override def tests = TestSuite {
    val testRef: dateMappedInput.Reference = dateMappedInput.ref()

    "Starts with given default value" - {
      val tester = createTestComponent(testRef)

      testRef().valueOrDefault ==> defaultDate
      tester.inputValue() ==> "2017-04-03"
    }

    "Updates input if value is set" - {
      val tester = createTestComponent(testRef)
      testRef().setValue(createDateTime(2017, MAY, 20))

      tester.inputValue() ==> "2017-05-20"
    }

    "Shows error if invalid value" - {
      val tester = createTestComponent(testRef, initialValue = "2017-02-40", showErrorMessage = true)

      tester.hasError ==> true
    }

    "ValueTransformer.StringToLocalDateTime.forward() works" - {
      val stringToLocalDateTime = MappedInput.ValueTransformer.StringToLocalDateTime
      stringToLocalDateTime.forward("2017-04-03") ==> Some(createDateTime(2017, APRIL, 3))
      stringToLocalDateTime.forward("2017-04-0333") ==> None
    }

    "ValueTransformer.StringToLocalDateTime.backward() works" - {
      val stringToLocalDateTime = MappedInput.ValueTransformer.StringToLocalDateTime
      stringToLocalDateTime.backward(createDateTime(2017, APRIL, 3)) ==> "2017-04-03"
    }
  }

  private def createTestComponent(ref: dateMappedInput.Reference,
                                  showErrorMessage: Boolean = false,
                                  initialValue: String = null): ComponentTester = {
    new ComponentTester(
      dateMappedInput(
        ref = ref,
        defaultValue = defaultDate,
        valueTransformer = MappedInput.ValueTransformer.StringToLocalDateTime,
        delegateRefFactory = TextInput.ref _
      ) { extraProps =>
        TextInput(
          ref = extraProps.ref,
          name = "dummy-name",
          label = "label",
          defaultValue = Option(initialValue) getOrElse extraProps.defaultValue,
          showErrorMessage = showErrorMessage,
          additionalValidator = extraProps.additionalValidator
        )
      }
    )
  }

  private final class ComponentTester(unrenderedComponent: VdomElement) {
    private val wrappedComponent = ReactTestWrapper.renderComponent(unrenderedComponent)

    def inputValue(): String = {
      wrappedComponent.child(tagName = "input").attribute("value")
    }

    def hasError: Boolean = {
      wrappedComponent.classes contains "has-error"
    }
  }
}
