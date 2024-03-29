package app.flux.react.uielements.input.bootstrap

import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.ScalaUtils
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputValidator
import hydro.flux.react.uielements.Bootstrap
import hydro.jsfacades.ReactTagInput
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.ext.KeyCode

import scala.collection.immutable.Seq

object TagInput {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps[State](props =>
      logExceptions { State(tags = props.defaultValue, listeners = Seq(props.listener)) }
    )
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(
      ref: Reference,
      label: String,
      name: String,
      suggestions: Seq[String],
      defaultValue: Seq[String],
      showErrorMessage: Boolean,
      additionalValidator: InputValidator[Seq[String]],
      inputClasses: Seq[String] = Seq(),
      listener: InputBase.Listener[Seq[String]] = InputBase.Listener.nullInstance,
  )(implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = defaultValue,
      suggestions = suggestions,
      showErrorMessage = showErrorMessage,
      additionalValidator = additionalValidator,
      inputClasses = inputClasses,
      listener = listener,
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[TagInput] (private[TagInput] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[Seq[String]] {
    override def apply(): InputBase.Proxy[Seq[String]] = {
      mutableRef.get.asCallback.runNow() map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }
  // **************** Private inner types ****************//
  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisMutableRef = ToScalaComponent[Props, State, Backend, ThisCtorSummoner#CT]
  private type ThisComponentU = MountedImpure[Props, State, Backend]

  case class Props(
      label: String,
      name: String,
      defaultValue: Seq[String],
      suggestions: Seq[String],
      showErrorMessage: Boolean,
      additionalValidator: InputValidator[Seq[String]],
      inputClasses: Seq[String],
      listener: InputBase.Listener[Seq[String]],
  )(implicit val i18n: I18n)

  case class State(tags: Seq[String], listeners: Seq[InputBase.Listener[Seq[String]]] = Seq()) {
    def withTags(newTags: Seq[String]): State = copy(tags = newTags)
    def withListener(listener: InputBase.Listener[Seq[String]]): State =
      copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener[Seq[String]]): State =
      copy(listeners = listeners.filter(_ != listener))
  }

  private final class Proxy(val component: ThisComponentU) extends InputBase.Proxy[Seq[String]] {
    override def value = {
      val value = component.state.tags
      ScalaUtils.ifThenOption(component.props.additionalValidator.isValid(value))(value)
    }
    override def valueOrDefault = value getOrElse Seq()
    override def setValue(newValue: Seq[String]) = {
      component.modState(_.withTags(newValue))
      for (listener <- component.state.listeners) {
        listener.onChange(newValue, directUserChange = false).runNow()
      }
      newValue
    }
    override def registerListener(listener: InputBase.Listener[Seq[String]]) =
      component.modState(_.withListener(listener))
    override def deregisterListener(listener: InputBase.Listener[Seq[String]]) = {
      component.modState(_.withoutListener(listener))
    }

    private def props: Props = component.props
  }

  private class Backend($ : BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      val errorMessage = generateErrorMessage(state, props)

      Bootstrap.FormGroup(
        ^^.ifThen(errorMessage.isDefined) {
          ^.className := "has-error"
        },
        Bootstrap.Col(sm = 4, tag = Bootstrap.ControlLabel(props.label)),
        Bootstrap.Col(sm = 8)(
          ReactTagInput(
            tags = state.tags,
            suggestions = props.suggestions,
            handleAddition = handleAddition,
            handleDelete = (pos, tag) => handleDelete(tag),
            handleDrag = handleDrag(_, _, _),
            delimiters = Seq(KeyCode.Enter, KeyCode.Tab, KeyCode.Space, /* comma */ 188, /* dot */ 190),
            minQueryLength = 1,
            classNames = Map("tagInputField" -> "form-control"),
          ),
          <<.ifDefined(errorMessage) { msg =>
            <.span(^.className := "help-block", msg)
          },
        ),
      )
    }

    private def handleAddition(newTag: String): Unit = logExceptions {
      // Don't add if duplicate
      handleChange(oldTags => if (oldTags contains newTag) oldTags else oldTags :+ newTag)
    }

    private def handleDelete(deletedTag: String): Unit = logExceptions {
      handleChange(tags => tags.filter(_ != deletedTag))
    }

    private def handleDrag(draggedTag: String, currentPos: Int, newPos: Int): Unit = logExceptions {
      handleChange(tags => {
        require(currentPos < tags.size, s"currentPos = $currentPos is not an index of $tags")
        require(newPos < tags.size, s"newPos = $newPos is not an index of $tags")
        require(tags(currentPos) == draggedTag, s"Expected ")

        val (index1, index2) = (currentPos, newPos)
        val (tag1, tag2) = (tags(index1), tags(index2))
        tags.zipWithIndex.map {
          case (_, index) if index == index1 => tag2
          case (_, index) if index == index2 => tag1
          case (tag, _)                      => tag
        }
      })
    }

    private def handleChange(tagsUpdate: Seq[String] => Seq[String]): Unit = {
      val state = $.state.runNow()
      val oldTags = state.tags
      val newTags = tagsUpdate(oldTags)

      if (oldTags != newTags) {
        for (listener <- state.listeners) {
          listener.onChange(newTags, directUserChange = true).runNow()
        }
        $.modState(_.withTags(newTags)).runNow()
      }
    }

    private def generateErrorMessage(state: State, props: Props): Option[String] = {
      if (props.showErrorMessage) {
        if (props.additionalValidator.isValid(state.tags)) {
          None
        } else {
          Some(props.i18n("error.invalid"))
        }
      } else {
        None
      }
    }
  }
}
