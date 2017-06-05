package flux.react.uielements.bootstrap

import common.CollectionUtils.toListMap
import common.GuavaReplacement.Iterables.getOnlyElement
import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag

class SelectInput[Value] private (implicit valueTag: ClassTag[Value]) {

  private val component = InputComponent.create[Value, ExtraProps](
    name = s"${getClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}",
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventFromInput => Callback,
                               extraProps: ExtraProps) = {
        <.select(
          ^^.classes(classes),
          ^.name := name,
          ^.value := valueString,
          ^.onChange ==> onChange,
          for ((optionId, option) <- extraProps.idToOptionMap) yield {
            <.option(
              ^.value := optionId,
              ^.key := optionId,
              option.name
            )
          }
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            label: String,
            defaultValue: Value = null.asInstanceOf[Value],
            inputClasses: Seq[String] = Seq(),
            options: Seq[Value],
            valueToId: Value => String,
            valueToName: Value => String,
            listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance)(
      implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = Option(defaultValue) getOrElse options.head,
      required = false,
      showErrorMessage = true, // Should never happen
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps.create(options, valueToId = valueToId, valueToName = valueToName),
      valueTransformer = ValueTransformer
    )
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[SelectInput] (refComp: InputComponent.ThisRefComp[Value, ExtraProps])
      extends InputComponent.Reference[Value, ExtraProps](refComp)

  case class ExtraProps private (idToOptionMap: Map[String, ExtraProps.ValueAndName])
  object ExtraProps {
    private[SelectInput] def create(options: Seq[Value],
                                    valueToId: Value => String,
                                    valueToName: Value => String): ExtraProps = {
      ExtraProps(
        idToOptionMap = toListMap {
          for (option <- options) yield {
            valueToId(option) -> ValueAndName(value = option, name = valueToName(option))
          }
        }
      )
    }

    case class ValueAndName private (value: Value, name: String)
  }

  // **************** Private inner types ****************//
  private object ValueTransformer extends InputComponent.ValueTransformer[Value, ExtraProps] {
    override def stringToValue(string: String, extraProps: ExtraProps) = {
      extraProps.idToOptionMap.get(string) map (_.value)
    }

    override def valueToString(value: Value, extraProps: ExtraProps) = {
      getOnlyElement(extraProps.idToOptionMap.filter { case (id, option) => option.value == value }.keys)
    }

    override def emptyValue = ???
  }
}

object SelectInput {
  private val typeToInstance: mutable.Map[Class[_], SelectInput[_]] = mutable.Map()

  def forType[Value: ClassTag]: SelectInput[Value] = {
    val clazz = implicitly[ClassTag[Value]].runtimeClass
    if (!(typeToInstance contains clazz)) {
      typeToInstance.put(clazz, new SelectInput[Value]())
    }
    typeToInstance(clazz).asInstanceOf[SelectInput[Value]]
  }
}
