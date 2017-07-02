package flux.react.uielements

import common.testing.{ReactTestWrapper, TestModule}
import flux.react.uielements
import flux.react.uielements.InputBase.Listener
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.vdom.VdomElement
import utest._

import scala.collection.mutable
import scala2js.Converters._

object InputWithDefaultFromReferenceTest extends TestSuite {
  implicit private val fake18n = new TestModule().fakeI18n
  private val stringInputWithDefault = InputWithDefaultFromReference.forType[String]
  private val testRef = stringInputWithDefault.ref()

  override def tests = TestSuite {
    val defaultValueProxy: InputBase.Proxy[String] = new FakeProxy()
    defaultValueProxy.setValue("startvalue")

    "Starts with same value as defaultValueProxy" - {
      val tester = createTestComponent(defaultValueProxy)

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true
    }

    "Starts with non-empty different from defaultValueProxy" - {
      defaultValueProxy.setValue("othervalue")

      val tester = createTestComponent(defaultValueProxy)

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> false
    }

    "Updates value if defaultValueProxy changes" - {
      val tester = createTestComponent(defaultValueProxy)

      defaultValueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }

    "No longer bound if own value changes" - {
      val tester = createTestComponent(defaultValueProxy)

      tester.valueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> false
    }

    "Binds again if own value changes to defaultValueProxy" - {
      val tester = createTestComponent(defaultValueProxy)
      tester.valueProxy.setValue("value2")
      tester.valueProxy.setValue("startvalue")

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true

    }

    "Binds again if defaultValueProxy changes to own value" - {
      val tester = createTestComponent(defaultValueProxy)
      tester.valueProxy.setValue("value2")
      defaultValueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }

    "Input name is given name" - {
      val tester = createTestComponent(defaultValueProxy)
      tester.inputName ==> "dummy-name"
    }
  }

  private def createTestComponent(proxy: InputBase.Proxy[String]): ComponentTester = {
    new ComponentTester(
      stringInputWithDefault(
        ref = testRef,
        defaultValueProxy = proxy,
        delegateRefFactory = uielements.bootstrap.TextInput.ref _) { extraProps =>
        uielements.bootstrap.TextInput(
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
    private val renderedComponent = ReactTestUtils.renderIntoDocument(unrenderedComponent)
    private val wrappedComponent = new ReactTestWrapper(renderedComponent)

    def valueProxy: InputBase.Proxy[String] = {
      testRef()
    }

    def showsBoundUntilChange: Boolean = {
      wrappedComponent.child(tagName = "input").classes contains "bound-until-change"
    }

    def inputName: String = {
      wrappedComponent.child(tagName = "input").attribute("name")
    }
  }
}
