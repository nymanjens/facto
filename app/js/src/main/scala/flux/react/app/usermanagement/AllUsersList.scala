package flux.react.app.usermanagement

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.<<
import flux.react.uielements
import flux.stores.{StateStore, UserStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.user.User

import scala.collection.immutable.Seq
import scala.scalajs.js

private[app] final class AllUsersList(implicit i18n: I18n, userStore: UserStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(maybeAllUsers = None))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(): VdomElement = {
    component()
  }

  // **************** Private inner types ****************//
  private type Props = Unit
  private case class State(maybeAllUsers: Option[Seq[User]])

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      userStore.register(this)
      $.modState(state => logExceptions(state.copy(maybeAllUsers = userStore.state.map(_.allUsers)))).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      userStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.copy(maybeAllUsers = userStore.state.map(_.allUsers)))).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      uielements.HalfPanel(title = <.span(i18n("facto.all-users"))) {
        uielements.Table(
          tableHeaders = Seq(
            <.th(i18n("facto.login-name")),
            <.th(i18n("facto.full-name")),
            <.th(i18n("facto.is-admin")),
            <.th(i18n("facto.expand-cash-flow")),
            <.th(i18n("facto.expand-liquidation"))
          ),
          tableRowDatas = tableRowDatas(state)
        )
      }
    }

    private def tableRowDatas(state: State): Seq[uielements.Table.TableRowData] = {
      state.maybeAllUsers match {
        case None =>
          for (i <- 0 until 3) yield {
            uielements.Table.TableRowData(
              Seq[VdomElement](
                <.td(^.colSpan := 5, ^.style := js.Dictionary("color" -> "white"), "loading...")))
          }
        case Some(allUsers) =>
          for (user <- allUsers) yield {
            uielements.Table.TableRowData(
              Seq[VdomElement](
                <.td(user.loginName),
                <.td(user.name),
                <.td(<<.ifThen(user.isAdmin)(<.i(^.className := "fa fa-check"))),
                <.td(<<.ifThen(user.expandCashFlowTablesByDefault)(<.i(^.className := "fa fa-check"))),
                <.td(<<.ifThen(user.expandLiquidationTablesByDefault)(<.i(^.className := "fa fa-check")))
              ))
          }
      }
    }
  }
}
