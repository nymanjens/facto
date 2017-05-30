package flux.react.uielements

import common.LoggingUtils.logExceptions
import common.testing.TestObjects._
import common.testing.{FakeRouterCtl, ReactTestWrapper, TestModule}
import flux.react.uielements
import flux.react.uielements.InputBase.Listener
import flux.stores.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.test.ReactTestUtils
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala2js.Converters._

object InputWithDefaultFromReferenceTest extends TestSuite {
  implicit val fake18n = new TestModule().fakeI18n

  override def tests = TestSuite {
    val defaultValueProxy: InputBase.Proxy[String] = new FakeProxy()
    defaultValueProxy.setValue("startvalue")

    "Starts with same value as defaultValueProxy" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true
    }

    "Starts with non-empty different from defaultValueProxy" - {
      defaultValueProxy.setValue("othervalue")

      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> false
    }

    "Updates value if defaultValueProxy changes" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))

      defaultValueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }

    "No longer bound if own value changes" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))

      tester.valueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> false
    }

    "Binds again if own value changes to defaultValueProxy" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))
      tester.valueProxy.setValue("value2")
      tester.valueProxy.setValue("startvalue")

      tester.valueProxy.valueOrDefault ==> "startvalue"
      tester.showsBoundUntilChange ==> true

    }

    "Binds again if defaultValueProxy changes to own value" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))
      tester.valueProxy.setValue("value2")
      defaultValueProxy.setValue("value2")

      tester.valueProxy.valueOrDefault ==> "value2"
      tester.showsBoundUntilChange ==> true
    }


    "Input name is given ref name" - {
      val tester = new ComponentTester(ReactTestComponent.component(ReactTestComponent.Props(defaultValueProxy)))
      tester.inputName ==> "testRef"
    }
  }

  object ReactTestComponent {
    val stringInputWithDefault = InputWithDefaultFromReference.forType[String]
    val testRef = stringInputWithDefault.ref("testRef")

    val component = ReactComponentB[Props](getClass.getSimpleName)
      .renderBackend[Backend]
      .build

    case class Props(proxy: InputBase.Proxy[String])

    class Backend(val $: BackendScope[Props, _]) {
      def render(props: Props) = logExceptions {
        stringInputWithDefault(
          ref = testRef,
          defaultValueProxy = props.proxy,
          nameToDelegateRef = uielements.bootstrap.TextInput.ref) {
          extraProps =>
            uielements.bootstrap.TextInput(
              ref = extraProps.ref,
              label = "label",
              defaultValue = "startvalue",
              showErrorMessage = false,
              inputClasses = extraProps.inputClasses
            )
        }
      }
    }
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

  private final class ComponentTester(unrenderedComponent: ReactComponentU[_, _, ReactTestComponent.Backend, _ <: TopNode]) {
    private val renderedComponent = ReactTestUtils.renderIntoDocument(unrenderedComponent)
    private val wrappedComponent = new ReactTestWrapper(renderedComponent)

    def valueProxy: InputBase.Proxy[String] = {
      ReactTestComponent.testRef(renderedComponent.backend.$)
    }

    def showsBoundUntilChange: Boolean = {
      wrappedComponent.child(tagName = "input").classes contains "bound-until-change"
    }

    def inputName: String = {
      wrappedComponent.child(tagName = "input").attribute("name")
    }
  }
}
