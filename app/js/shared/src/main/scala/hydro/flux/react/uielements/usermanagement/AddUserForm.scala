package hydro.flux.react.uielements.usermanagement

import app.api.ScalaJsApi.UserPrototype
import hydro.common.I18n
import app.models.user.User
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.input.bootstrap.TextInput
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

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

    private val loginNameRef = TextInput.ref()
    private val nameRef = TextInput.ref()
    private val passwordRef = TextInput.ref()
    private val passwordVerificationRef = TextInput.ref()

    override def render(props: Props, state: State) = logExceptions {
      Bootstrap.FormHorizontal(
        HalfPanel(title = <.span(i18n("app.add-user")))(
          {
            for (error <- state.globalErrors) yield {
              Bootstrap.Alert(variant = Variant.danger)(^.key := error, error)
            }
          }.toVdomArray,
          TextInput(
            ref = loginNameRef,
            name = "loginName",
            label = i18n("app.login-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          TextInput(
            ref = nameRef,
            name = "name",
            label = i18n("app.full-name"),
            required = true,
            showErrorMessage = state.showErrorMessages
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
          Bootstrap.Button(tpe = "submit")(^.onClick ==> onSubmit, i18n("app.add"))
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
              dispatcher.dispatch(StandardActions.UpsertUser(userPrototype))

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
