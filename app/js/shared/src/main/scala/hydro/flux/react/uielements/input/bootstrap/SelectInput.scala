package hydro.flux.react.uielements.input.bootstrap

import hydro.common.CollectionUtils.toListMap
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag

class SelectInput[Value] private (implicit valueTag: ClassTag[Value]) {

  private val component = InputComponent.create[Value, ExtraProps](
    name = s"${getClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}",
    inputRenderer = (
        classes: Seq[String],
        name: String,
        valueString: String,
        onChange: String => Callback,
        extraProps: ExtraProps,
    ) => {
      <.select(
        ^^.classes(classes),
        ^.name := name,
        ^.value := valueString,
        ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value)), {
          for ((optionId, option) <- extraProps.idToOptionMap) yield {
            <.option(
              ^.value := optionId,
              ^.key := optionId,
              option.name,
            )
          }
        }.toVdomArray,
      )
    },
  )

  // **************** API ****************//
  def apply(
      ref: Reference,
      name: String,
      label: String,
      defaultValue: Value = null.asInstanceOf[Value],
      inputClasses: Seq[String] = Seq(),
      options: Seq[Value],
      valueToId: Value => String,
      valueToName: Value => String,
      listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance,
  )(implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = Option(defaultValue) getOrElse options.head,
      required = false,
      showErrorMessage = true, // Should never happen
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps.create(options, valueToId = valueToId, valueToName = valueToName),
      valueTransformer = ValueTransformer,
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[SelectInput] (
      private[SelectInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps]
  ) extends InputComponent.Reference[Value, ExtraProps](mutableRef)

  case class ExtraProps private (idToOptionMap: Map[String, ExtraProps.ValueAndName])
  object ExtraProps {
    private[SelectInput] def create(
        options: Seq[Value],
        valueToId: Value => String,
        valueToName: Value => String,
    ): ExtraProps = {
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

    override def isEmptyValue(value: Value) = false
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
