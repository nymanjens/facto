package hydro.flux.react.uielements.usermanagement

import app.api.ScalaJsApi.UserPrototype
import app.common.I18n
import app.models.user.User
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.input.bootstrap.TextInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[usermanagement] final class UpdatePasswordForm(implicit user: User,
                                                       i18n: I18n,
                                                       dispatcher: Dispatcher)
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

    private val passwordRef = TextInput.ref()
    private val passwordVerificationRef = TextInput.ref()

    override def render(props: Props, state: State) = logExceptions {
      <.form(
        ^.className := "form-horizontal",
        HalfPanel(title = <.span(i18n("app.change-password")))(
          {
            for (error <- state.globalErrors) yield {
              <.div(^.className := "alert alert-danger", ^.key := error, error)
            }
          }.toVdomArray,
          TextInput(
            ref = TextInput.ref(),
            name = "loginName",
            label = i18n("app.login-name"),
            defaultValue = user.loginName,
            disabled = true
          ),
          TextInput(
            ref = passwordRef,
            name = "password",
            label = i18n("app.password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          TextInput(
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
            i18n("app.ok"))
        )
      )
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      val props = $.props.runNow()
      e.preventDefault()

      $.modState(state =>
        logExceptions {
          var newState = State(showErrorMessages = true)

          val maybeNewPassword = for {
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
          } yield validPassword

          maybeNewPassword match {
            case Some(newPassword) =>
              dispatcher.dispatch(
                StandardActions.UpsertUser(
                  UserPrototype.create(id = user.id, plainTextPassword = newPassword)))

              // Clear form
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
