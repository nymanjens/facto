package flux.react.app.balancecheckform

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import common.LoggingUtils.logExceptions
import common.money.ExchangeRateManager
import common.time.Clock
import common.time.LocalDateTime
import flux.action.Actions
import hydro.flux.action.StandardActions
import hydro.flux.action.Dispatcher
import flux.react.ReactVdomUtils.<<
import flux.router.RouterContext
import flux.react.uielements
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.WaitForFuture
import flux.react.uielements.input.bootstrap.MoneyInput
import flux.react.uielements.input.MappedInput
import hydro.flux.react.uielements.input.bootstrap.TextInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.accounting.BalanceCheck
import models.accounting.config.Config
import models.accounting.config.MoneyReservoir
import models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class BalanceCheckForm(implicit i18n: I18n,
                             clock: Clock,
                             accountingConfig: Config,
                             user: User,
                             entityAccess: EntityAccess,
                             exchangeRateManager: ExchangeRateManager,
                             dispatcher: Dispatcher,
                             pageHeader: PageHeader,
) {

  private val waitForFuture = new WaitForFuture[Props]
  private val dateMappedInput = MappedInput.forTypes[String, LocalDateTime]

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState(State(showErrorMessages = false))
      .renderBackend[Backend]
      .build
  }

  // **************** API ****************//
  def forCreate(reservoirCode: String, returnToPath: Path, router: RouterContext): VdomElement = {
    create(Props(OperationMeta.AddNew(accountingConfig.moneyReservoir(reservoirCode)), returnToPath, router))
  }

  def forEdit(balanceCheckId: Long, returnToPath: Path, router: RouterContext): VdomElement =
    create(async {
      val balanceCheck = await(entityAccess.newQuery[BalanceCheck]().findById(balanceCheckId))
      Props(OperationMeta.Edit(balanceCheck), returnToPath, router)
    })

  // **************** Private helper methods ****************//
  private def create(props: Props): VdomElement = create(Future.successful(props))
  private def create(propsFuture: Future[Props]): VdomElement = {
    waitForFuture(futureInput = propsFuture) { props =>
      component.withKey(props.operationMeta.toString).apply(props)
    }
  }

  // **************** Private inner types ****************//
  private sealed trait OperationMeta {
    def reservoir: MoneyReservoir
    def issuer: User
    def checkDate: LocalDateTime
    def balanceInCents: Long
  }
  private object OperationMeta {
    case class AddNew(override val reservoir: MoneyReservoir) extends OperationMeta {
      override def issuer = user
      override def checkDate = clock.now
      override def balanceInCents = 0
    }
    case class Edit(balanceCheck: BalanceCheck) extends OperationMeta {
      override def reservoir = balanceCheck.moneyReservoir
      override def issuer = balanceCheck.issuer
      override def checkDate = balanceCheck.checkDate
      override def balanceInCents = balanceCheck.balanceInCents
    }
  }

  private case class State(showErrorMessages: Boolean)

  private case class Props(operationMeta: OperationMeta, returnToPath: Path, router: RouterContext)

  private final class Backend(val $ : BackendScope[Props, State]) {
    private val checkDateRef = dateMappedInput.ref()
    private val balanceRef = MoneyInput.ref()

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.div(
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-lg-12",
            pageHeader.withExtension(router.currentPage)(
              <<.ifThen(props.operationMeta.isInstanceOf[OperationMeta.Edit]) {
                <.span(
                  " ",
                  <.a(
                    ^.className := "btn btn-default delete-button",
                    <.i(^.className := "fa fa-times"),
                    " ",
                    i18n("app.delete"),
                    ^.onClick --> onDelete
                  ))
              }
            )
          )
        ),
        <.div(
          ^.className := "row",
          <.form(
            ^.className := "form-horizontal",
            HalfPanel(title = <.span(i18n("app.balance-check")))(
              TextInput(
                ref = TextInput.ref(),
                name = "issuer",
                label = i18n("app.issuer"),
                defaultValue = props.operationMeta.issuer.name,
                disabled = true
              ),
              TextInput(
                ref = TextInput.ref(),
                name = "money-reservoir",
                label = i18n("app.reservoir"),
                defaultValue = props.operationMeta.reservoir.name,
                disabled = true
              ),
              dateMappedInput(
                ref = checkDateRef,
                defaultValue = props.operationMeta.checkDate,
                valueTransformer = MappedInput.ValueTransformer.StringToLocalDateTime,
                delegateRefFactory = TextInput.ref _
              ) { mappedExtraProps =>
                TextInput(
                  ref = mappedExtraProps.ref,
                  name = "check-date",
                  label = i18n("app.check-date"),
                  defaultValue = mappedExtraProps.defaultValue,
                  required = true,
                  showErrorMessage = state.showErrorMessages,
                  additionalValidator = mappedExtraProps.additionalValidator
                )
              },
              MoneyInput(
                ref = balanceRef,
                name = "balance",
                label = i18n("app.balance"),
                defaultValue = props.operationMeta.balanceInCents,
                required = false, // not required to be different from 0.0
                showErrorMessage = state.showErrorMessages,
                currency = props.operationMeta.reservoir.currency
              )
            ),
            <.div(
              ^.className := "form-group",
              <.div(
                ^.className := "col-sm-offset-2 col-sm-10",
                <.button(
                  ^.tpe := "submit",
                  ^.className := "btn btn-default",
                  ^.onClick ==> onSubmit,
                  i18n("app.ok")
                )
              )
            )
          )
        )
      )
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      val props = $.props.runNow()

      def submitValid(balanceCheckWithoutId: BalanceCheck) = {
        val action = props.operationMeta match {
          case OperationMeta.AddNew(_) =>
            Actions.AddBalanceCheck(balanceCheckWithoutId)
          case OperationMeta.Edit(existingBalanceCheck) =>
            Actions.UpdateBalanceCheck(
              existingBalanceCheck = existingBalanceCheck,
              newBalanceCheckWithoutId = balanceCheckWithoutId)
        }

        dispatcher.dispatch(action)
      }

      e.preventDefault()

      $.modState(state =>
        logExceptions {
          val newState = state.copy(showErrorMessages = true)

          val maybeBalanceCheck = for {
            checkDate <- checkDateRef().value
            balance <- balanceRef().value
          } yield
            BalanceCheck(
              issuerId = user.id,
              moneyReservoirCode = props.operationMeta.reservoir.code,
              balanceInCents = balance,
              createdDate = clock.now,
              checkDate = checkDate)

          maybeBalanceCheck match {
            case Some(balanceCheckWithoutId) =>
              submitValid(balanceCheckWithoutId)
              props.router.setPath(props.returnToPath)
            case None =>
          }
          newState
      }).runNow()
    }

    private def onDelete: Callback = LogExceptionsCallback {
      val props = $.props.runNow()
      props.operationMeta match {
        case OperationMeta.AddNew(_) => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(balanceCheck) =>
          dispatcher.dispatch(Actions.RemoveBalanceCheck(existingBalanceCheck = balanceCheck))
          props.router.setPath(props.returnToPath)
      }
    }
  }
}
