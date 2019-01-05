package app.flux.react.uielements.input

import hydro.common.testing.ReactTestWrapper
import app.common.testing.TestModule
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputBase.Listener
import hydro.flux.react.uielements.input.bootstrap.TextInput
import japgolly.scalajs.react.vdom.VdomElement
import utest._

import scala.collection.mutable

object InputWithDefaultFromReferenceTest extends TestSuite {
  implicit private val fake18n = new TestModule().fakeI18n
  private val stringInputWithDefault = InputWithDefaultFromReference.forType[String]

  override def tests = TestSuite {
    val testRef: stringInputWithDefault.Reference = stringInputWithDefault.ref()
    val defaultValueProxy: InputBase.Proxy[String] = new FakeProxy()
    defaultValueProxy.setValue("startvalue")

    "Starts with same value as defaultValueProxy" - {
      val tester = createTestComponent(testRef, defaultValueProxy)

      testRef().valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true
    }

    "Starts with non-empty different from defaultValueProxy" - {
      defaultValueProxy.setValue("othervalue")

      val tester = createTestComponent(testRef, defaultValueProxy)

      testRef().valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> false
    }

    "Updates value if defaultValueProxy changes" - {
      val tester = createTestComponent(testRef, defaultValueProxy)

      defaultValueProxy.setValue("value2")

      testRef().valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }

    "No longer bound if own value changes" - {
      val tester = createTestComponent(testRef, defaultValueProxy)

      testRef().setValue("value2")

      testRef().valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> false
    }

    "Binds again if own value changes to defaultValueProxy" - {
      val tester = createTestComponent(testRef, defaultValueProxy)
      testRef().setValue("value2")
      testRef().setValue("startvalue")

      testRef().valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true

    }

    "Binds again if defaultValueProxy changes to own value" - {
      val tester = createTestComponent(testRef, defaultValueProxy)
      testRef().setValue("value2")
      defaultValueProxy.setValue("value2")

      testRef().valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }

    "Input name is given name" - {
      val tester = createTestComponent(testRef, defaultValueProxy)
      tester.inputName ==> "dummy-name"
    }
  }

  private def createTestComponent(ref: stringInputWithDefault.Reference,
                                  proxy: InputBase.Proxy[String]): ComponentTester = {
    new ComponentTester(
      stringInputWithDefault(ref = ref, defaultValueProxy = proxy, delegateRefFactory = TextInput.ref _) {
        extraProps =>
          TextInput(
            ref = extraProps.ref,
            name = "dummy-name",
            label = "label",
            defaultValue = "startvalue",
            showErrorMessage = false,
            inputClasses = extraProps.inputClasses
          )
      }
    )
  }

  private final class FakeProxy extends InputBase.Proxy[String] {
    private val listeners: mutable.Buffer[Listener[String]] = mutable.Buffer()
    private var _value: Option[String] = None

    override def value = _value
    override def valueOrDefault: String = _value getOrElse ""
    override def setValue(newValue: String): String = {
      _value = Some(newValue)
      for (listener <- listeners) {
        listener.onChange(newValue, directUserChange = false).runNow()
      }
      newValue
    }
    override def registerListener(listener: Listener[String]): Unit = {
      listeners += listener
    }
    override def deregisterListener(listener: Listener[String]): Unit = {
      listeners -= listener
    }
  }

  private final class ComponentTester(unrenderedComponent: VdomElement) {
    private val wrappedComponent = ReactTestWrapper.renderComponent(unrenderedComponent)

    def showsBoundUntilChange: Boolean = {
      wrappedComponent.child(tagName = "input").classes contains "bound-until-change"
    }

    def inputName: String = {
      wrappedComponent.child(tagName = "input").attribute("name")
    }
  }
}
