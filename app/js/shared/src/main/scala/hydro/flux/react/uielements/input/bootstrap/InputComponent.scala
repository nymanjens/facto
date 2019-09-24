package hydro.flux.react.uielements.input.bootstrap

import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputValidator
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
  * ReactScala component that can be used as building block for input components that fit
  * in a Bootstrap row.
  */
object InputComponent {

  // **************** API ****************//
  def create[Value, ExtraProps](
      name: String,
      valueChangeForPropsChange: (Props[Value, ExtraProps], Value) => Value =
        (_: Props[Value, ExtraProps], oldValue: Value) => oldValue,
      inputRenderer: InputRenderer[ExtraProps],
  ) = {
    ScalaComponent
      .builder[Props[Value, ExtraProps]](name)
      .initialStateFromProps[State[Value]](props =>
        logExceptions {
          // Calling valueChangeForPropsChange() to make sure there is no discrepancy between init and update.
          val value = valueChangeForPropsChange(props, props.defaultValue)
          State(valueString = ValueTransformer.valueToString(value, props), listeners = Seq(props.listener))
      })
      .renderPS((context, props, state) =>
        logExceptions {
          def onChange(newString: String): Callback = LogExceptionsCallback {
            val newValue = ValueTransformer.stringToValueOrDefault(newString, props)
            val oldValue = ValueTransformer.stringToValueOrDefault(state.valueString, props)
            if (oldValue != newValue) {
              for (listener <- context.state.listeners) {
                listener.onChange(newValue, directUserChange = true).runNow()
              }
            }
            context.modState(_.withValueString(newString)).runNow()
          }
          val errorMessage = generateErrorMessage(state, props)

          Bootstrap.FormGroup(
            ^^.ifThen(errorMessage.isDefined) {
              ^.className := "has-error"
            },
            Bootstrap.Col(sm = 4, tag = Bootstrap.ControlLabel(props.label)),
            Bootstrap.Col(sm = 8)(
              inputRenderer.renderInput(
                classes = "form-control" +: props.inputClasses,
                name = props.name,
                valueString = state.valueString,
                onChange = onChange,
                extraProps = props.extra),
              <<.ifDefined(errorMessage) { msg =>
                <.span(^.className := "help-block", msg)
              }
            )
          )
      })
      .componentWillReceiveProps(scope =>
        LogExceptionsCallback {
          // If the props have changed, the transformed value may have changed. If this happens, the listeners should
          // be notified.
          val valueString = scope.state.valueString
          val currentValue = ValueTransformer.stringToValueOrDefault(valueString, scope.currentProps)
          val newValue = {
            val transformedValue = ValueTransformer.stringToValueOrDefault(valueString, scope.nextProps)
            valueChangeForPropsChange(scope.nextProps, transformedValue)
          }
          if (currentValue != newValue) {
            scope
              .modState(_.withValueString(ValueTransformer.valueToString(newValue, scope.nextProps)))
              .runNow()
            for (listener <- scope.state.listeners) {
              listener.onChange(newValue, directUserChange = false).runNow()
            }
          }
      })
      .build
  }

  // **************** Private methods ****************//
  private def generateErrorMessage[Value, ExtraProps](
      state: State[Value],
      props: Props[Value, ExtraProps],
  ): Option[String] = {
    if (props.showErrorMessage) {
      def isEmpty(value: Value): Boolean = props.valueTransformer.isEmptyValue(value)

      ValueTransformer.stringToValue(state.valueString, props) match {
        case None => Some(props.i18n("error.invalid"))
        case Some(value) if props.required && isEmpty(value) =>
          Some(props.i18n("error.required"))
        case Some(value) if !isEmpty(value) && !props.additionalValidator.isValid(value) =>
          Some(props.i18n("error.invalid"))
        case _ => None
      }
    } else {
      None
    }
  }

  // **************** Public inner types ****************//
  private type ThisCtorSummoner[Value, ExtraProps] =
    CtorType.Summoner.Aux[Box[Props[Value, ExtraProps]], Children.None, CtorType.Props]
  type ThisMutableRef[Value, ExtraProps] =
    ToScalaComponent[Props[Value, ExtraProps], State[Value], Backend, ThisCtorSummoner[Value, ExtraProps]#CT]

  trait InputRenderer[ExtraProps] {
    def renderInput(
        classes: Seq[String],
        name: String,
        valueString: String,
        onChange: String => Callback,
        extraProps: ExtraProps,
    ): VdomNode
  }

  /** Converter between String and Value. */
  trait ValueTransformer[Value, -ExtraProps] {

    /**
      * Returns the Value that corresponds to the given String or None iff the current value is
      * invalid (excluding the case where the default value is invalid).
      */
    def stringToValue(string: String, extraProps: ExtraProps): Option[Value]

    /**
      * Returns the string value of given value.
      *
      * @throws Exception (any) if the given value is invalid for given props
      */
    def valueToString(value: Value, extraProps: ExtraProps): String

    /** Return true if the given value represents an empty input. */
    def isEmptyValue(value: Value): Boolean
  }

  object ValueTransformer {
    def nullInstance[ExtraProps]: ValueTransformer[String, ExtraProps] =
      new ValueTransformer[String, ExtraProps] {
        override def stringToValue(string: String, extraProps: ExtraProps) = Some(string)
        override def valueToString(value: String, extraProps: ExtraProps) = value
        override def isEmptyValue(value: String) = value == ""
      }

    def stringToValue[Value, ExtraProps](string: String, props: Props[Value, ExtraProps]): Option[Value] = {
      props.valueTransformer.stringToValue(string, props.extra)
    }

    def stringToValueOrDefault[Value, ExtraProps](string: String, props: Props[Value, ExtraProps]): Value = {
      val maybeValue = stringToValue(string, props)
      maybeValue getOrElse props.defaultValue
    }

    def valueToString[Value, ExtraProps](value: Value, props: Props[Value, ExtraProps]): String = {
      props.valueTransformer.valueToString(value, props.extra)
    }
  }

  case class Props[Value, ExtraProps](
      label: String,
      name: String,
      defaultValue: Value,
      required: Boolean,
      showErrorMessage: Boolean,
      additionalValidator: InputValidator[Value] = InputValidator.alwaysValid[Value],
      inputClasses: Seq[String],
      listener: InputBase.Listener[Value],
      extra: ExtraProps = (): Unit,
      valueTransformer: ValueTransformer[Value, ExtraProps],
  )(implicit val i18n: I18n)

  case class State[Value](valueString: String, listeners: Seq[InputBase.Listener[Value]] = Seq()) {
    def withValueString(newString: String): State[Value] = copy(valueString = newString)
    def withListener(listener: InputBase.Listener[Value]): State[Value] =
      copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener[Value]): State[Value] =
      copy(listeners = listeners.filter(_ != listener))
  }

  abstract class Reference[Value, ExtraProps](mutableRef: ThisMutableRef[Value, ExtraProps])
      extends InputBase.Reference[Value] {
    override final def apply(): InputBase.Proxy[Value] = {
      mutableRef.get.asCallback.runNow() map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }

  // **************** Private inner types ****************//
  private type Backend = Unit
  private type ThisComponentU[Value, ExtraProps] =
    MountedImpure[Props[Value, ExtraProps], State[Value], Backend]

  private final class Proxy[Value, ExtraProps](val component: ThisComponentU[Value, ExtraProps])
      extends InputBase.Proxy[Value] {
    override def value = {
      ValueTransformer.stringToValue(component.state.valueString, props) match {
        case Some(value) if props.required && props.valueTransformer.isEmptyValue(value) => None
        case other                                                                       => other
      }
    }
    override def valueOrDefault =
      ValueTransformer.stringToValueOrDefault(component.state.valueString, props)
    override def setValue(newValue: Value) = {
      def setValueInternal(newValue: Value): Value = {
        val stringValue = ValueTransformer.valueToString(newValue, props)
        component.modState(_.withValueString(stringValue))
        for (listener <- component.state.listeners) {
          listener.onChange(newValue, directUserChange = false).runNow()
        }
        newValue
      }
      val maybeStringValue = Try(ValueTransformer.valueToString(newValue, props))
      maybeStringValue match {
        case Success(stringValue) =>
          val valueThroughTransformer = ValueTransformer.stringToValueOrDefault(stringValue, props)
          if (newValue == valueThroughTransformer) {
            setValueInternal(newValue)
          } else {
            console.log(
              s"  Setting a value ('$newValue') that is different when transformed to string and back to value " +
                s"(valueThroughTransformer = '$valueThroughTransformer'). Will ignore this setter.")
            this.valueOrDefault
          }
        case Failure(e) =>
          console.log(
            s"  Failed to get the String value for $newValue. This may be intended if the valid options for " +
              s"this input change. Will ignore this setter.\n$e")
          this.valueOrDefault
      }
    }
    override def registerListener(listener: InputBase.Listener[Value]) =
      component.modState(_.withListener(listener))
    override def deregisterListener(listener: InputBase.Listener[Value]) = {
      component.modState(_.withoutListener(listener))
    }

    private def props: Props[Value, ExtraProps] = component.props
  }
}
