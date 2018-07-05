package flux.react.app.usermanagement

import api.ScalaJsApi.UserPrototype
import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.{Action, Dispatcher}
import flux.react.uielements
import flux.react.uielements.input.bootstrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.user.User

import scala.collection.immutable.Seq

private[usermanagement] final class AddUserForm(implicit user: User, i18n: I18n, dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State())
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Private inner types ****************//
  private case class Props()
  private case class State(showErrorMessages: Boolean = false, globalErrors: Seq[String] = Seq())

  private final class Backend(val $ : BackendScope[Props, State]) {

    private val loginNameRef = bootstrap.TextInput.ref()
    private val nameRef = bootstrap.TextInput.ref()
    private val passwordRef = bootstrap.TextInput.ref()
    private val passwordVerificationRef = bootstrap.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
      <.form(
        ^.className := "form-horizontal",
        uielements.HalfPanel(title = <.span(i18n("facto.add-user")))(
          {
            for (error <- state.globalErrors) yield {
              <.div(^.className := "alert alert-danger", ^.key := error, error)
            }
          }.toVdomArray,
          bootstrap.TextInput(
            ref = loginNameRef,
            name = "loginName",
            label = i18n("facto.login-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = nameRef,
            name = "name",
            label = i18n("facto.full-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = passwordRef,
            name = "password",
            label = i18n("facto.password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = passwordVerificationRef,
            name = "passwordVerification",
            label = i18n("facto.retype-password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          <.button(
            ^.tpe := "submit",
            ^.className := "btn btn-default",
            ^.onClick ==> onSubmit,
            i18n("facto.add"))
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
                newState = newState.copy(globalErrors = Seq(i18n("facto.error.passwords-should-match")))
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
