package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, LogExceptionsFuture, logExceptions}
import common.money.{Currency, ExchangeRateManager, ReferenceMoney}
import common.time.Clock
import common.time.JavaTimeImplicits._
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import flux.react.router.RouterContext
import flux.react.uielements
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.accounting.config.Config
import models.accounting.{Transaction, TransactionGroup}
import models.user.User

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TransactionGroupForm(implicit i18n: I18n,
                                 clock: Clock,
                                 accountingConfig: Config,
                                 user: User,
                                 entityAccess: EntityAccess,
                                 exchangeRateManager: ExchangeRateManager,
                                 dispatcher: Dispatcher,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel,
                                 totalFlowInput: TotalFlowInput,
                                 totalFlowRestrictionInput: TotalFlowRestrictionInput) {

  private val waitForFuture = new uielements.WaitForFuture[Props]
  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialStateFromProps(props =>
        logExceptions {
          val numberOfTransactions = props.groupPartial.transactions.length
          val totalFlowRestriction = props.groupPartial match {
            case partial if partial.zeroSum => TotalFlowRestriction.ZeroSum
            case _                          => TotalFlowRestriction.AnyTotal
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
  def forCreate(returnToPath: Path, router: RouterContext): VdomElement = {
    forCreate(TransactionGroup.Partial.withSingleEmptyTransaction, returnToPath, router)
  }

  def forEdit(transactionGroupId: Long, returnToPath: Path, router: RouterContext): VdomElement =
    create(async {
      val group = await(entityAccess.newQuery[TransactionGroup]().findById(transactionGroupId))
      val transactions = await(group.transactions)

      Props(
        operationMeta = OperationMeta.Edit(group, transactions),
        groupPartial = TransactionGroup.Partial.from(group, transactions),
        returnToPath = returnToPath,
        router = router
      )
    })

  def forReservoir(reservoirCode: String, returnToPath: Path, router: RouterContext): VdomElement = {
    val reservoir = accountingConfig.moneyReservoir(reservoirCode)
    forCreate(
      TransactionGroup.Partial(
        Seq(
          Transaction.Partial.from(
            beneficiary = reservoir.owner,
            moneyReservoir = reservoir
          )
        )
      ),
      returnToPath,
      router
    )
  }

  def forTemplate(templateCode: String, returnToPath: Path, router: RouterContext): VdomElement = {
    val template = accountingConfig.templateWithCode(templateCode)
    // If this user is not associated with an account, it should not see any templates.
    val userAccount = accountingConfig.accountOf(user).get
    forCreate(template.toPartial(userAccount), returnToPath, router)
  }

  def forRepayment(accountCode1: String,
                   accountCode2: String,
                   returnToPath: Path,
                   router: RouterContext): VdomElement = {
    val account1 = accountingConfig.accounts(accountCode1)
    val account2 = accountingConfig.accounts(accountCode2)

    forCreate(
      TransactionGroup.Partial(
        Seq(
          Transaction.Partial.from(
            beneficiary = account1,
            moneyReservoir = account1.defaultElectronicReservoir,
            category = accountingConfig.constants.accountingCategory,
            description = accountingConfig.constants.liquidationDescription
          ),
          Transaction.Partial.from(
            beneficiary = account1,
            moneyReservoir = account2.defaultElectronicReservoir,
            category = accountingConfig.constants.accountingCategory,
            description = accountingConfig.constants.liquidationDescription
          )
        ),
        zeroSum = true
      ),
      returnToPath,
      router
    )
  }

  // **************** Private helper methods ****************//
  private def forCreate(transactionGroupPartial: TransactionGroup.Partial,
                        returnToPath: Path,
                        router: RouterContext): VdomElement = {
    create(
      Props(
        operationMeta = OperationMeta.AddNew,
        groupPartial = transactionGroupPartial,
        returnToPath = returnToPath,
        router = router))
  }

  private def create(props: Props): VdomElement = create(Future.successful(props))
  private def create(propsFuture: Future[Props]): VdomElement = {
    waitForFuture(futureInput = propsFuture) { props =>
      component.withKey(props.operationMeta.toString).apply(props)
    }
  }

  // **************** Private inner types ****************//
  private sealed trait OperationMeta
  private object OperationMeta {
    case object AddNew extends OperationMeta
    case class Edit(group: TransactionGroup, transactions: Seq[Transaction]) extends OperationMeta
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

  private case class Props(operationMeta: OperationMeta,
                           groupPartial: TransactionGroup.Partial,
                           returnToPath: Path,
                           router: RouterContext)

  private final class Backend(val $ : BackendScope[Props, State]) {

    private val _panelRefs: mutable.Buffer[transactionPanel.Reference] =
      mutable.Buffer(transactionPanel.ref())

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.div(
        ^.className := "transaction-group-form",
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-lg-12",
            uielements.PageHeader.withExtension(router.currentPage)(
              <<.ifThen(props.operationMeta.isInstanceOf[OperationMeta.Edit]) {
                <.a(
                  ^.className := "btn btn-default delete-button",
                  <.i(^.className := "fa fa-times"),
                  " ",
                  i18n("app.delete"),
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
                val transactionPartial = props.groupPartial.transactions match {
                  case transactions if panelIndex < transactions.size =>
                    transactions.apply(panelIndex)
                  case _ => Transaction.Partial.empty
                }
                transactionPanel(
                  key = panelIndex,
                  ref = panelRef(panelIndex),
                  title = i18n("app.transaction") + " " + (i + 1),
                  defaultValues = transactionPartial,
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
                  i18n("app.ok")
                )
              )
            )
          )
        )
      )
    }

    private def panelRef(panelIndex: Int): transactionPanel.Reference = {
      while (panelIndex >= _panelRefs.size) {
        _panelRefs += transactionPanel.ref()
      }
      _panelRefs(panelIndex)
    }

    private val addTransactionPanelCallback: Callback = LogExceptionsCallback {
      $.modState(state => logExceptions(state.plusPanel())).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(state => logExceptions(state.minusPanelIndex(index))).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def updateTotalFlow(totalFlow: ReferenceMoney): Unit = {
      $.modState(_.copy(totalFlow = totalFlow)).runNow()
    }

    private def updateTotalFlowRestriction(totalFlowRestriction: TotalFlowRestriction): Unit = {
      $.modState(state =>
        logExceptions {
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
            foreignCurrency = currencies.find(_.isForeign),
            totalFlowExceptLast = flows.dropRight(1).sum)
          if (state.totalFlowRestriction == TotalFlowRestriction.AnyTotal) {
            newState = newState.copy(totalFlow = flows.sum)
          }
          newState
      }).runNow()
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      val props = $.props.runNow()

      def getErrorMessage(datas: Seq[transactionPanel.Data], state: State): Option[String] = {
        def invalidMoneyReservoirsError: Option[String] = {
          val containsEmptyReservoirCodes = datas.exists(_.moneyReservoir.isNullReservoir)
          val allReservoirCodesAreEmpty = datas.forall(_.moneyReservoir.isNullReservoir)

          datas.size match {
            case 0                                => throw new AssertionError("Should not be possible")
            case 1 if containsEmptyReservoirCodes => Some(i18n("app.error.noReservoir.atLeast2"))
            case _ =>
              if (containsEmptyReservoirCodes) {
                if (allReservoirCodesAreEmpty) {
                  if (state.totalFlow.isZero) {
                    None
                  } else {
                    Some(i18n("app.error.noReservoir.zeroSum"))
                  }
                } else {
                  Some(i18n("app.error.noReservoir.notAllTheSame"))
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
            Some(i18n("app.error.foreignReservoirInFuture"))
          } else {
            None
          }
        }
        invalidMoneyReservoirsError orElse noFutureForeignTransactionsError
      }

      def submitValid(datas: Seq[transactionPanel.Data], state: State) = {
        def transactionsWithoutIdProvider(group: TransactionGroup, issuerId: Option[Long] = None) = {
          for (data <- datas)
            yield
              Transaction(
                transactionGroupId = group.id,
                issuerId = issuerId getOrElse user.id,
                beneficiaryAccountCode = data.beneficiaryAccount.code,
                moneyReservoirCode = data.moneyReservoir.code,
                categoryCode = data.category.code,
                description = data.description,
                flowInCents = data.flow.cents,
                detailDescription = data.detailDescription,
                tags = data.tags,
                createdDate = group.createdDate,
                transactionDate = data.transactionDate,
                consumedDate = data.consumedDate
              )
        }

        val action = props.operationMeta match {
          case OperationMeta.AddNew =>
            Action.AddTransactionGroup(transactionsWithoutIdProvider = transactionsWithoutIdProvider(_))
          case OperationMeta.Edit(group, transactions) =>
            Action.UpdateTransactionGroup(
              transactionGroupWithId = group,
              transactionsWithoutId = transactionsWithoutIdProvider(group, Some(transactions.head.issuerId))
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
                props.router.setPath(props.returnToPath)
            }
          }

          newState
      }).runNow()
    }

    private def onDelete: Callback = LogExceptionsCallback {
      val props = $.props.runNow()

      props.operationMeta match {
        case OperationMeta.AddNew => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(group, transactions) =>
          dispatcher.dispatch(Action.RemoveTransactionGroup(transactionGroupWithId = group))
          props.router.setPath(props.returnToPath)
      }
    }
  }
}
