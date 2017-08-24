package flux.react.app.balancecheckform

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.time.{Clock, LocalDateTime}
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.^^
import flux.react.router.Page
import flux.react.uielements.HalfPanel
import flux.react.uielements.input.bootstrap.MoneyInput
import flux.react.uielements.input.{MappedInput, bootstrap}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.BalanceCheck
import models.accounting.config.{Config, MoneyReservoir}
import models.accounting.money.ExchangeRateManager
import models.{EntityAccess, User}

final class BalanceCheckForm(implicit i18n: I18n,
                             clock: Clock,
                             accountingConfig: Config,
                             user: User,
                             balanceCheckManager: BalanceCheck.Manager,
                             entityAccess: EntityAccess,
                             exchangeRateManager: ExchangeRateManager,
                             dispatcher: Dispatcher) {

  private val dateMappedInput = MappedInput.forTypes[String, LocalDateTime]

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState(State(showErrorMessages = false))
      .renderBackend[Backend]
      .build
  }

  // **************** API ****************//
  def forCreate(reservoirCode: String, router: RouterCtl[Page]): VdomElement = {
    create(Props(OperationMeta.AddNew(accountingConfig.moneyReservoir(reservoirCode)), router))
  }

  def forEdit(balanceCheckId: Long, router: RouterCtl[Page]): VdomElement = {
    val balanceCheck = balanceCheckManager.findById(balanceCheckId)
    create(Props(OperationMeta.Edit(balanceCheck), router))
  }

  // **************** Private helper methods ****************//
  private def create(props: Props): VdomElement = {
    component.withKey(props.operationMeta.toString).apply(props)
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

  private case class Props(operationMeta: OperationMeta, router: RouterCtl[Page])

  private final class Backend(val $ : BackendScope[Props, State]) {
    val checkDateRef = dateMappedInput.ref()
    val balanceRef = MoneyInput.ref()

    def render(props: Props, state: State) = logExceptions {
      <.div(
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-lg-12",
            <.h1(
              ^.className := "page-header",
              <.i(^.className := "icon-new-empty"),
              props.operationMeta match {
                case OperationMeta.AddNew(_) => i18n("facto.new-balance-check")
                case OperationMeta.Edit(_) => i18n("facto.edit-balance-check")
              },
              ^^.ifThen(props.operationMeta.isInstanceOf[OperationMeta.Edit]) {
                <.a(
                  ^.className := "btn btn-default delete-button",
                  <.i(^.className := "fa fa-times"),
                  i18n("facto.delete"),
                  ^.onClick --> onDelete
                )
              }
            )
          )
        ),
        <.div(
          ^.className := "row",
          <.form(
            ^.className := "form-horizontal",
            HalfPanel(title = <.span(i18n("facto.balance-check")))(
              bootstrap.TextInput(
                ref = bootstrap.TextInput.ref(),
                name = "issuer",
                label = i18n("facto.issuer"),
                defaultValue = props.operationMeta.issuer.name,
                disabled = true
              ),
              bootstrap.TextInput(
                ref = bootstrap.TextInput.ref(),
                name = "money-reservoir",
                label = i18n("facto.reservoir"),
                defaultValue = props.operationMeta.reservoir.name,
                disabled = true
              ),
              dateMappedInput(
                ref = checkDateRef,
                defaultValue = props.operationMeta.checkDate,
                valueTransformer = MappedInput.ValueTransformer.StringToLocalDateTime,
                delegateRefFactory = bootstrap.TextInput.ref _
              ) { mappedExtraProps =>
                bootstrap.TextInput(
                  ref = mappedExtraProps.ref,
                  name = "check-date",
                  label = i18n("facto.check-date"),
                  defaultValue = mappedExtraProps.defaultValue,
                  required = true,
                  showErrorMessage = state.showErrorMessages,
                  additionalValidator = mappedExtraProps.additionalValidator
                )
              },
              bootstrap.MoneyInput(
                ref = balanceRef,
                name = "balance",
                label = i18n("facto.balance"),
                defaultValue = props.operationMeta.balanceInCents,
                required = true,
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
                  i18n("facto.ok")
                )
              )
            )
          )
        )
      )
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      def submitValid(balanceCheckWithoutId: BalanceCheck) = {
        val action = $.props.runNow().operationMeta match {
          case OperationMeta.AddNew(_) =>
            Action.AddBalanceCheck(balanceCheckWithoutId)
          case OperationMeta.Edit(existingBalanceCheck) =>
            Action.UpdateBalanceCheck(balanceCheckWithoutId.withId(existingBalanceCheck.id))
        }

        dispatcher.dispatch(action)
      }

      e.preventDefault()

      $.modState(state =>
        logExceptions {
          val props = $.props.runNow()
          var newState = state.copy(showErrorMessages = true)

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
              $.props.runNow().router.set(Page.EverythingPage).runNow()
            case None =>
          }
          newState
      }).runNow()
    }

    private def onDelete: Callback = LogExceptionsCallback {
      $.props.runNow().operationMeta match {
        case OperationMeta.AddNew(_) => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(balanceCheck) =>
          dispatcher.dispatch(Action.RemoveBalanceCheck(balanceCheckWithId = balanceCheck))
          $.props.runNow().router.set(Page.EverythingPage).runNow()
      }
    }
  }
}
