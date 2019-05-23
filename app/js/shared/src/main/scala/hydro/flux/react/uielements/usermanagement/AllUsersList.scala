package hydro.flux.react.uielements.usermanagement

import hydro.common.I18n
import app.models.user.User
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.Table
import hydro.flux.stores.UserStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

import scala.collection.immutable.Seq
import scala.scalajs.js

final class AllUsersList(implicit i18n: I18n, userStore: UserStore) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(userStore, _.copy(maybeAllUsers = userStore.state.map(_.allUsers)))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(maybeAllUsers: Option[Seq[User]] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      HalfPanel(title = <.span(i18n("app.all-users"))) {
        Table(
          tableHeaders = Seq(
            <.th(i18n("app.login-name")),
            <.th(i18n("app.full-name")),
            <.th(i18n("app.is-admin")),
          ),
          tableRowDatas = tableRowDatas(state)
        )
      }
    }

    private def tableRowDatas(state: State): Seq[Table.TableRowData] = {
      state.maybeAllUsers match {
        case None =>
          for (i <- 0 until 3) yield {
            Table.TableRowData(
              Seq[VdomElement](
                <.td(^.colSpan := 5, ^.style := js.Dictionary("color" -> "white"), "loading...")))
          }
        case Some(allUsers) =>
          for (user <- allUsers) yield {
            Table.TableRowData(
              Seq[VdomElement](
                <.td(user.loginName),
                <.td(user.name),
                <.td(<<.ifThen(user.isAdmin)(Bootstrap.FontAwesomeIcon("check"))),
              ))
          }
      }
    }
  }
}
