package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, LogExceptionsFuture, logExceptions}
import common.time.Clock
import common.time.JavaTimeImplicits._
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.^^
import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import flux.react.router.Page
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.config.Config
import models.accounting.money.{Currency, ExchangeRateManager, ReferenceMoney}
import models.accounting.{Tag, Transaction, TransactionGroup}
import models.{EntityAccess, User}

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TransactionGroupForm(implicit i18n: I18n,
                                 clock: Clock,
                                 accountingConfig: Config,
                                 user: User,
                                 transactionGroupManager: TransactionGroup.Manager,
                                 entityAccess: EntityAccess,
                                 exchangeRateManager: ExchangeRateManager,
                                 dispatcher: Dispatcher,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel,
                                 totalFlowInput: TotalFlowInput,
                                 totalFlowRestrictionInput: TotalFlowRestrictionInput) {

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialStateFromProps(props =>
        logExceptions {
          val numberOfTransactions = props.operationMeta match {
            case OperationMeta.AddNew => 1
            case OperationMeta.Edit(group) => group.transactions.length
          }
          val totalFlowRestriction = props.operationMeta match {
            case OperationMeta.Edit(group) if group.isZeroSum => TotalFlowRestriction.ZeroSum
            case _ => TotalFlowRestriction.AnyTotal
          }
          State(
            panelIndices = 0 until numberOfTransactions,
            nextPanelIndex = numberOfTransactions,
            // The following fields are updated by onFormChange() when the component is mounted
            foreignCurrency = None,
            totalFlowRestriction = totalFlowRestriction,
            totalFlow = ReferenceMoney(0),
            totalFlowExceptLast = ReferenceMoney(0)
          )
      })
      .renderBackend[Backend]
      .componentDidMount(scope => LogExceptionsCallback(scope.backend.onFormChange()))
      .build
  }

  // **************** API ****************//
  def forCreate(router: RouterCtl[Page]): VdomElement = {
    create(Props(OperationMeta.AddNew, router))
  }

  def forEdit(transactionGroupId: Long, router: RouterCtl[Page]): VdomElement = {
    create(Props(OperationMeta.Edit(transactionGroupManager.findById(transactionGroupId)), router))
  }

  // **************** Private helper methods ****************//
  private def create(props: Props): VdomElement = {
    component.withKey(props.operationMeta.toString).apply(props)
  }

  private def panelRef(panelIndex: Int): transactionPanel.Reference =
    transactionPanel.ref()

  // **************** Private inner types ****************//
  private sealed trait OperationMeta
  private object OperationMeta {
    case object AddNew extends OperationMeta
    case class Edit(group: TransactionGroup) extends OperationMeta
  }

  /**
    * @param foreignCurrency Any foreign currency of any of the selected money reservoirs. If there are multiple,
    *                        this can by any of these.
    */
  private case class State(panelIndices: Seq[Int],
                           nextPanelIndex: Int,
                           showErrorMessages: Boolean = false,
                           globalErrorMessage: Option[String] = None,
                           foreignCurrency: Option[Currency],
                           totalFlowRestriction: TotalFlowRestriction,
                           totalFlow: ReferenceMoney,
                           totalFlowExceptLast: ReferenceMoney) {
    def plusPanel(): State =
      copy(panelIndices = panelIndices :+ nextPanelIndex, nextPanelIndex = nextPanelIndex + 1)
    def minusPanelIndex(index: Int): State = copy(panelIndices = panelIndices.filter(_ != index))
  }

  private case class Props(operationMeta: OperationMeta, router: RouterCtl[Page])

  private final class Backend(val $ : BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      <.div(
        ^.className := "transaction-group-form",
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-lg-12",
            <.h1(
              ^.className := "page-header",
              <.i(^.className := "icon-new-empty"),
              props.operationMeta match {
                case OperationMeta.AddNew => i18n("facto.new-transaction")
                case OperationMeta.Edit(_) => i18n("facto.edit-transaction")
              },
              ^^.ifThen(props.operationMeta.isInstanceOf[OperationMeta.Edit]) {
                <.a(
                  ^.className := "btn btn-default delete-button",
                  <.i(^.className := "fa fa-times"),
                  i18n("facto.delete"),
                  ^.onClick --> onDelete
                )
              },
              <.span(
                ^.className := "total-transaction-flow-box",
                totalFlowInput(
                  forceValue =
                    if (state.totalFlowRestriction == TotalFlowRestriction.ChooseTotal) None
                    else Some(state.totalFlow),
                  foreignCurrency = state.foreignCurrency,
                  onChange = updateTotalFlow
                ),
                totalFlowRestrictionInput(
                  defaultValue = state.totalFlowRestriction,
                  onChange = updateTotalFlowRestriction
                )
              )
            )
          )
        ),
        ^^.ifThen(state.globalErrorMessage) { errorMessage =>
          <.div(
            ^.className := "alert alert-danger",
            errorMessage
          )
        },
        <.form(
          ^.className := "form-horizontal",
          ^.key := "main-form",
          <.div(
            ^.className := "row",
            <.div(
              ^.className := "transaction-group-form",
              (for ((panelIndex, i) <- state.panelIndices.zipWithIndex) yield {
                val firstPanel = panelIndex == state.panelIndices.head
                val lastPanel = panelIndex == state.panelIndices.last
                val transaction = props.operationMeta match {
                  case OperationMeta.Edit(group) if panelIndex < group.transactions.length =>
                    Some(group.transactions.apply(panelIndex))
                  case _ => None
                }
                transactionPanel(
                  key = panelIndex,
                  ref = panelRef(panelIndex),
                  title = i18n("facto.transaction") + " " + (i + 1),
                  defaultValues = transaction,
                  forceFlowValue = if (lastPanel && state.totalFlowRestriction.userSetsTotal) {
                    Some(state.totalFlow - state.totalFlowExceptLast)
                  } else {
                    None
                  },
                  showErrorMessages = state.showErrorMessages,
                  defaultPanel =
                    if (firstPanel) None else Some(panelRef(panelIndex = state.panelIndices.head)()),
                  focusOnMount = firstPanel,
                  closeButtonCallback = if (firstPanel) None else Some(removeTransactionPanel(panelIndex)),
                  onFormChange = this.onFormChange _
                )
              }).toVdomArray,
              addTransactionPanel(onClick = addTransactionPanelCallback)
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

    private val addTransactionPanelCallback: Callback = LogExceptionsCallback {
      $.modState(logExceptions(_.plusPanel())).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(logExceptions(_.minusPanelIndex(index))).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def updateTotalFlow(totalFlow: ReferenceMoney): Unit = {
      $.modState(_.copy(totalFlow = totalFlow)).runNow()
    }

    private def updateTotalFlowRestriction(totalFlowRestriction: TotalFlowRestriction): Unit = {
      $.modState(logExceptions { state =>
        var newState = state.copy(totalFlowRestriction = totalFlowRestriction)
        if (totalFlowRestriction == TotalFlowRestriction.ZeroSum) {
          newState = newState.copy(totalFlow = ReferenceMoney(0))
        }
        newState
      }).runNow()
    }

    def onFormChange(): Unit = {
      $.modState(state =>
        logExceptions {
          val flows = for (panelIndex <- state.panelIndices) yield {
            val datedMoney = panelRef(panelIndex).apply().flowValueOrDefault
            datedMoney.exchangedForReferenceCurrency
          }
          val currencies = for (panelIndex <- state.panelIndices) yield {
            panelRef(panelIndex).apply().moneyReservoir.valueOrDefault.currency
          }

          var newState = state.copy(
            foreignCurrency = currencies.filter(_.isForeign).headOption,
            totalFlowExceptLast = flows.dropRight(1).sum)
          if (state.totalFlowRestriction == TotalFlowRestriction.AnyTotal) {
            newState = newState.copy(totalFlow = flows.sum)
          }
          newState
      }).runNow()
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      def getErrorMessage(datas: Seq[transactionPanel.Data], state: State): Option[String] = {
        def invalidMoneyReservoirsError: Option[String] = {
          val containsEmptyReservoirCodes = datas.exists(_.moneyReservoir.isNullReservoir)
          val allReservoirCodesAreEmpty = datas.forall(_.moneyReservoir.isNullReservoir)

          datas.size match {
            case 0 => throw new AssertionError("Should not be possible")
            case 1 if containsEmptyReservoirCodes => Some(i18n("facto.error.noReservoir.atLeast2"))
            case _ =>
              if (containsEmptyReservoirCodes) {
                if (allReservoirCodesAreEmpty) {
                  if (state.totalFlow.isZero) {
                    None
                  } else {
                    Some(i18n("facto.error.noReservoir.zeroSum"))
                  }
                } else {
                  Some(i18n("facto.error.noReservoir.notAllTheSame"))
                }
              } else {
                None
              }
          }
        }

        // Don't allow future transactions in a foreign currency because we don't know what the exchange rate
        // to the default currency will be. Future fluctuations might break the immutability of the conversion.
        def noFutureForeignTransactionsError: Option[String] = {
          val futureForeignTransactionsExist = datas.exists { data =>
            val foreignCurrency = data.moneyReservoir.currency.isForeign
            val dateInFuture = data.transactionDate > clock.now
            foreignCurrency && dateInFuture
          }
          if (futureForeignTransactionsExist) {
            Some(i18n("facto.error.foreignReservoirInFuture"))
          } else {
            None
          }
        }
        invalidMoneyReservoirsError orElse noFutureForeignTransactionsError
      }

      def submitValid(datas: Seq[transactionPanel.Data], state: State) = {
        def transactionsWithoutIdProvider(group: TransactionGroup) = {
          for (data <- datas)
            yield
              Transaction(
                transactionGroupId = group.id,
                issuerId = user.id,
                beneficiaryAccountCode = data.beneficiaryAccount.code,
                moneyReservoirCode = data.moneyReservoir.code,
                categoryCode = data.category.code,
                description = data.description,
                flowInCents = data.flow.cents,
                detailDescription = data.detailDescription,
                tagsString = Tag.serializeToString(data.tags),
                createdDate = group.createdDate,
                transactionDate = data.transactionDate,
                consumedDate = data.consumedDate
              )
        }

        val action = $.props.runNow().operationMeta match {
          case OperationMeta.AddNew =>
            Action.AddTransactionGroup(transactionsWithoutIdProvider = transactionsWithoutIdProvider)
          case OperationMeta.Edit(group) =>
            Action.UpdateTransactionGroup(
              transactionGroupWithId = group,
              transactionsWithoutId = transactionsWithoutIdProvider(group)
            )
        }

        dispatcher.dispatch(action)
      }

      e.preventDefault()

      $.modState(state =>
        logExceptions {
          var newState = state.copy(showErrorMessages = true)

          val maybeDatas = for (panelIndex <- state.panelIndices) yield panelRef(panelIndex).apply().data
          if (maybeDatas forall (_.isDefined)) {
            val datas = maybeDatas map (_.get)

            getErrorMessage(datas, state) match {
              case Some(errorMessage) =>
                newState = newState.copy(globalErrorMessage = Some(errorMessage))

              case None =>
                submitValid(datas, state)
                $.props.runNow().router.set(Page.EverythingPage).runNow()
            }
          }

          newState
      }).runNow()
    }

    private def onDelete: Callback = LogExceptionsCallback {
      $.props.runNow().operationMeta match {
        case OperationMeta.AddNew => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(group) =>
          dispatcher.dispatch(Action.RemoveTransactionGroup(transactionGroupWithId = group))
          $.props.runNow().router.set(Page.EverythingPage).runNow()
      }
    }
  }
}
