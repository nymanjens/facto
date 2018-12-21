package flux.react.app.usermanagement

import api.ScalaJsApi.UserPrototype
import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.{Action, Dispatcher}
import flux.react.common.HydroReactComponent
import flux.react.uielements
import flux.react.uielements.input.bootstrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.user.User

import scala.collection.immutable.Seq

private[usermanagement] final class AddUserForm(implicit user: User, i18n: I18n, dispatcher: Dispatcher)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()
  protected case class State(showErrorMessages: Boolean = false, globalErrors: Seq[String] = Seq())

  protected final class Backend(val $ : BackendScope[Props, State]) extends BackendBase($) {

    private val loginNameRef = bootstrap.TextInput.ref()
    private val nameRef = bootstrap.TextInput.ref()
    private val passwordRef = bootstrap.TextInput.ref()
    private val passwordVerificationRef = bootstrap.TextInput.ref()

    override def render(props: Props, state: State) = logExceptions {
      <.form(
        ^.className := "form-horizontal",
        uielements.HalfPanel(title = <.span(i18n("app.add-user")))(
          {
            for (error <- state.globalErrors) yield {
              <.div(^.className := "alert alert-danger", ^.key := error, error)
            }
          }.toVdomArray,
          bootstrap.TextInput(
            ref = loginNameRef,
            name = "loginName",
            label = i18n("app.login-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = nameRef,
            name = "name",
            label = i18n("app.full-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = passwordRef,
            name = "password",
            label = i18n("app.password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = passwordVerificationRef,
            name = "passwordVerification",
            label = i18n("app.retype-password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          <.button(
            ^.tpe := "submit",
            ^.className := "btn btn-default",
            ^.onClick ==> onSubmit,
            i18n("app.add"))
        )
      )
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      val props = $.props.runNow()
      e.preventDefault()

      $.modState(state =>
        logExceptions {
          var newState = State(showErrorMessages = true)

          val maybeUserPrototype = for {
            loginName <- loginNameRef().value
            name <- nameRef().value
            password <- passwordRef().value
            passwordVerification <- passwordVerificationRef().value
            validPassword <- {
              if (password != passwordVerification) {
                newState = newState.copy(globalErrors = Seq(i18n("app.error.passwords-should-match")))
                None
              } else {
                Some(password)
              }
            }
          } yield UserPrototype.create(loginName = loginName, name = name, plainTextPassword = validPassword)

          maybeUserPrototype match {
            case Some(userPrototype) =>
              dispatcher.dispatch(Action.UpsertUser(userPrototype))

              // Clear form
              loginNameRef().setValue("")
              nameRef().setValue("")
              passwordRef().setValue("")
              passwordVerificationRef().setValue("")
              newState = State()

            case None =>
          }
          newState
      }).runNow()
    }
  }
}
