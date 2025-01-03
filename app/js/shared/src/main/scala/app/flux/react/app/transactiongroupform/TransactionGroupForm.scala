package app.flux.react.app.transactiongroupform

import scala.scalajs.js
import org.scalajs.dom.raw.ClipboardEvent
import app.common.money.Currency
import app.common.money.CurrencyValueManager
import app.common.money.ReferenceMoney
import app.common.AttachmentFormatting
import app.flux.action.AppActions
import app.flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import app.flux.react.app.transactionviews.Liquidation
import app.flux.router.AppPages
import app.flux.router.AppPages.PopupEditorPage
import app.flux.stores.entries.AccountPair
import app.flux.stores.entries.factories.LiquidationEntriesStoreFactory
import app.flux.stores.AttachmentStore
import app.models.access.AppJsEntityAccess
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.Transaction.Attachment
import app.models.user.User
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.LogExceptionsFuture
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.common.ScalaUtils.ifThenOption
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.WaitForFuture
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom
import japgolly.scalajs.react.vdom.all.EmptyVdom
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.FileReader

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.util.Failure
import scala.util.Success

final class TransactionGroupForm(implicit
    i18n: I18n,
    clock: Clock,
    accountingConfig: Config,
    user: User,
    entityAccess: AppJsEntityAccess,
    currencyValueManager: CurrencyValueManager,
    dispatcher: Dispatcher,
    transactionPanel: TransactionPanel,
    addTransactionPanel: AddTransactionPanel,
    totalFlowInput: TotalFlowInput,
    totalFlowRestrictionInput: TotalFlowRestrictionInput,
    liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
    pageHeader: PageHeader,
    attachmentStore: AttachmentStore,
) extends HydroReactComponent {

  private val waitForFuture = new WaitForFuture[Props]

  // **************** API ****************//
  def forCreate(router: RouterContext): VdomElement = {
    forCreate(TransactionGroup.Partial.withSingleEmptyTransaction, router)
  }

  def forEdit(transactionGroupId: Long, router: RouterContext): VdomElement =
    create(async {
      val group = await(entityAccess.newQuery[TransactionGroup]().findById(transactionGroupId))
      val transactions = await(group.transactions)

      Props(
        operationMeta = OperationMeta.Edit(group, transactions),
        groupPartial = TransactionGroup.Partial.from(group, transactions),
        router = router,
      )
    })

  def forCreateFromCopy(transactionGroupId: Long, router: RouterContext): VdomElement = {
    def clearedUncopyableFields(groupPartial: TransactionGroup.Partial): TransactionGroup.Partial = {
      val clearedTransactions = for (transaction <- groupPartial.transactions) yield {
        Transaction.Partial(
          beneficiary = transaction.beneficiary,
          moneyReservoir = transaction.moneyReservoir,
          category = transaction.category,
          description = transaction.description,
          flowInCents = transaction.flowInCents,
          detailDescription = transaction.detailDescription,
          tags = transaction.tags,
        )
      }

      TransactionGroup.Partial(
        transactions = clearedTransactions,
        zeroSum = groupPartial.zeroSum,
      )
    }

    create(async {
      val group = await(entityAccess.newQuery[TransactionGroup]().findById(transactionGroupId))
      val transactions = await(group.transactions)

      Props(
        operationMeta = OperationMeta.AddNew,
        groupPartial = clearedUncopyableFields(TransactionGroup.Partial.from(group, transactions)),
        router = router,
      )
    })
  }

  def forReservoir(reservoirCode: String, router: RouterContext): VdomElement = {
    val reservoir = accountingConfig.moneyReservoir(reservoirCode)
    forCreate(
      TransactionGroup.Partial(
        Seq(
          Transaction.Partial.from(
            beneficiary = reservoir.owner,
            moneyReservoir = reservoir,
          )
        )
      ),
      router,
    )
  }

  def forTemplate(templateCode: String, router: RouterContext): VdomElement = {
    val template = accountingConfig.templateWithCode(templateCode)
    // If this user is not associated with an account, it should not see any templates.
    val userAccount = accountingConfig.accountOf(user).get
    forCreate(template.toPartial(userAccount), router)
  }

  def forRepayment(accountCode1: String, accountCode2: String, router: RouterContext): VdomElement = {
    val account1 = accountingConfig.accounts(accountCode1)
    val account2 = accountingConfig.accounts(accountCode2)

    forCreate(
      TransactionGroup.Partial(
        Seq(
          Transaction.Partial.from(
            beneficiary = account1,
            moneyReservoir = account1.defaultElectronicReservoir,
            category = accountingConfig.constants.accountingCategory,
            description = accountingConfig.constants.liquidationDescription,
          ),
          Transaction.Partial.from(
            beneficiary = account1,
            moneyReservoir = account2.defaultElectronicReservoir,
            category = accountingConfig.constants.accountingCategory,
            description = accountingConfig.constants.liquidationDescription,
          ),
        ),
        zeroSum = true,
      ),
      router,
    )
  }

  def forLiquidationSimplification(router: RouterContext): VdomElement =
    create(async {
      def props(groupPartial: TransactionGroup.Partial): Props =
        Props(
          operationMeta = OperationMeta.AddNew,
          groupPartial = groupPartial,
          router = router,
        )
      val commonAccount = accountingConfig.constants.commonAccount

      val pairToDebt: Map[AccountPair, Option[ReferenceMoney]] =
        await(Future.sequence(for {
          (account1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
          (account2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
          accountPair <- Some(AccountPair(account1, account2))
          if i1 < i2 && !accountPair.toSet.contains(commonAccount)
        } yield {
          val stateFuture =
            liquidationEntriesStoreFactory.get(accountPair, Liquidation.minNumEntriesPerPair).stateFuture
          stateFuture.map { state =>
            accountPair -> state.entries.lastOption.map(_.entry.debt)
          }
        })).toMap

      val filteredPairToDebt = (for {
        (accountPair, maybeDebt) <- pairToDebt
        debt <- maybeDebt
        if debt.nonZero
      } yield accountPair -> debt).toMap

      if (filteredPairToDebt.nonEmpty) {
        def partial(
            title: String,
            beneficiary: Account,
            reservoirAccount: Account,
            flow: ReferenceMoney,
        ): Transaction.Partial =
          Transaction.Partial(
            beneficiary = Some(beneficiary),
            moneyReservoir = Some(reservoirAccount.defaultElectronicReservoir),
            category = Some(accountingConfig.constants.accountingCategory),
            description = title,
            flowInCents = flow.cents,
          )

        val partialTransactions = for {
          (AccountPair(account1, account2), debt) <- filteredPairToDebt.toVector
          transaction <- {
            val title = i18n("app.liquidation-simplification", account1.veryShortName, account2.veryShortName)
            Seq(
              partial(title, beneficiary = commonAccount, reservoirAccount = account1, flow = debt),
              partial(title, beneficiary = account1, reservoirAccount = account1, flow = -debt),
              partial(title, beneficiary = commonAccount, reservoirAccount = account2, flow = -debt),
              partial(title, beneficiary = account1, reservoirAccount = account2, flow = debt),
            )
          }
        } yield transaction

        props(TransactionGroup.Partial(transactions = partialTransactions, zeroSum = true))
      } else {
        props(TransactionGroup.Partial.withSingleEmptyTransaction)
      }
    })

  // **************** Private helper methods ****************//
  private def forCreate(
      transactionGroupPartial: TransactionGroup.Partial,
      router: RouterContext,
  ): VdomElement = {
    create(
      Props(
        operationMeta = OperationMeta.AddNew,
        groupPartial = transactionGroupPartial,
        router = router,
      )
    )
  }

  private def create(props: Props): VdomElement = create(Future.successful(props))
  private def create(propsFuture: Future[Props]): VdomElement = {
    waitForFuture(futureInput = propsFuture) { props =>
      component.withKey(props.operationMeta.toString).apply(props)
    }
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(
    backendConstructor = new Backend(_),
    initialStateFromProps = props => {
      val numberOfTransactions = props.groupPartial.transactions.length
      val totalFlowRestriction = props.groupPartial match {
        case partial if partial.zeroSum => TotalFlowRestriction.ZeroSum
        case _                          => TotalFlowRestriction.AnyTotal
      }
      State(
        panelIndices = 0 until numberOfTransactions,
        flowFractions = (0 until numberOfTransactions).map(_ => 0.0),
        nextPanelIndex = numberOfTransactions,
        // The following fields are updated by onFormChange() when the component is mounted
        foreignCurrency = None,
        totalFlowRestriction = totalFlowRestriction,
        totalFlow = ReferenceMoney(0),
        totalFlowExceptLast = ReferenceMoney(0),
        attachments = props.groupPartial.transactions.flatMap(t => t.attachments).distinct,
        attachmentsPendingUpload = Seq(),
      )
    },
  )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      operationMeta: OperationMeta,
      groupPartial: TransactionGroup.Partial,
      router: RouterContext,
  )

  /**
   * @param foreignCurrency Any foreign currency of any of the selected money reservoirs. If there are multiple,
   *                        this can by any of these.
   */
  protected case class State(
      panelIndices: Seq[Int],
      flowFractions: Seq[Double],
      nextPanelIndex: Int,
      showErrorMessages: Boolean = false,
      globalErrorMessage: Option[String] = None,
      foreignCurrency: Option[Currency],
      totalFlowRestriction: TotalFlowRestriction,
      totalFlow: ReferenceMoney,
      totalFlowExceptLast: ReferenceMoney,
      attachments: Seq[Attachment],
      attachmentsPendingUpload: Seq[State.AttachmentPendingUpload],
  ) {
    def plusPanel(panelRef: Int => transactionPanel.Reference): State = {
      copy(
        panelIndices = panelIndices :+ nextPanelIndex,
        flowFractions = flowFractions :+ 0.0,
        nextPanelIndex = nextPanelIndex + 1,
      ).withRefreshedFlowFractions(panelRef)
    }

    def minusPanelIndex(index: Int, panelRef: Int => transactionPanel.Reference): State = {
      copy(panelIndices = panelIndices.filter(_ != index)).withRefreshedFlowFractions(panelRef)
    }

    def withRefreshedFlowFractions(panelRef: Int => transactionPanel.Reference): State = {
      val flows = for (panelIndex <- panelIndices) yield {
        val datedMoney = panelRef(panelIndex).apply().flowValueOrDefault
        datedMoney.exchangedForReferenceCurrency()
      }
      val sumOfFlow = flows.sum
      copy(
        flowFractions = flows.map(flow => if (sumOfFlow.isZero) 0.0 else flow.toDouble / sumOfFlow.toDouble)
      )
    }

    def containsAttachment(pendingAttachment: State.AttachmentPendingUpload): Boolean = {
      attachmentsPendingUpload.contains(pendingAttachment) || attachments.exists(a =>
        pendingAttachment.toAttachment(a.contentHash) == a
      )
    }
  }
  protected object State {
    case class AttachmentPendingUpload(filename: String, fileType: String, fileSizeBytes: Int) {
      def toAttachment(hash: String): Attachment = {
        Attachment(
          filename = filename,
          contentHash = hash,
          fileType = fileType,
          fileSizeBytes = fileSizeBytes,
        )
      }
    }
  }

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with WillUnmount {

    private val _panelRefs: mutable.Buffer[transactionPanel.Reference] =
      mutable.Buffer(transactionPanel.ref())

    private val onPasteEventListener: js.Function1[ClipboardEvent, Unit] = e => onPasteEvent(e)

    override def didMount(props: Props, state: State): Callback = {
      dom.window.addEventListener("paste", onPasteEventListener)
      onFormChange()
      Callback.empty
    }

    override def willUnmount(props: Props, state: State): Callback = {
      dom.window.removeEventListener("paste", onPasteEventListener)
      Callback.empty
    }

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router
      <.div(
        ^.className := "transaction-group-form",
        Bootstrap.Row(
          Bootstrap.Col(lg = 12)(
            pageHeader.withExtension(router.currentPage)(
              <<.ifThen(props.operationMeta.isInstanceOf[OperationMeta.Edit]) {
                <.span(
                  Bootstrap.Button(tag = <.a)(
                    ^.className := "delete-button",
                    Bootstrap.FontAwesomeIcon("trash-o"),
                    " ",
                    i18n("app.delete"),
                    ^.onClick --> onDelete,
                  ),
                  " ",
                  Bootstrap.Button(tag = <.a)(
                    Bootstrap.FontAwesomeIcon("copy"),
                    ^.onClick --> onCopy,
                  ),
                )
              },
              <.span(
                ^.className := "total-transaction-flow-box",
                totalFlowInput(
                  forceValue =
                    if (state.totalFlowRestriction == TotalFlowRestriction.ChooseTotal) None
                    else Some(state.totalFlow),
                  foreignCurrency = state.foreignCurrency,
                  onChange = updateTotalFlow,
                ),
                totalFlowRestrictionInput(
                  defaultValue = state.totalFlowRestriction,
                  onChange = updateTotalFlowRestriction,
                ),
              ),
            )
          )
        ),
        ^^.ifDefined(state.globalErrorMessage) { errorMessage =>
          Bootstrap.Alert(Variant.danger)(
            errorMessage
          )
        },
        <<.ifThen(state.attachments.nonEmpty || state.attachmentsPendingUpload.nonEmpty) {
          <.div(
            ^.className := "attachments", {
              for (attachment <- state.attachments)
                yield attachmentDiv(attachment.filename, attachment.fileSizeBytes, attachment)
            }.toVdomArray, {
              for (attachment <- state.attachmentsPendingUpload)
                yield attachmentDiv(attachment.filename, attachment.fileSizeBytes)
            }.toVdomArray,
          )
        },
        Bootstrap.FormHorizontal(
          ^.key := "main-form",
          ^.encType := "multipart/form-data",
          Bootstrap.Row(
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
                  fractionToShow = ifThenOption(
                    state.panelIndices.size >= 2 && !state.foreignCurrency.isDefined && !state.totalFlow.isZero
                  ) {
                    state.flowFractions(i)
                  },
                  showErrorMessages = state.showErrorMessages,
                  defaultPanel =
                    if (firstPanel) None else Some(panelRef(panelIndex = state.panelIndices.head)()),
                  focusOnMount = firstPanel,
                  closeButtonCallback = if (firstPanel) None else Some(removeTransactionPanel(panelIndex)),
                  onFormChange = this.onFormChange _,
                )
              }).toVdomArray,
              addTransactionPanel(onClick = addTransactionPanelCallback),
            ),
            Bootstrap.FormGroup(
              Bootstrap.Col(sm = 10, smOffset = 2)(
                Bootstrap.Button(tpe = "submit")(
                  ^.onClick ==> onSubmit(redirectOnSuccess = true),
                  i18n("app.submit"),
                ),
                " ",
                <<.ifThen(props.operationMeta == OperationMeta.AddNew) {
                  Bootstrap.Button()(
                    ^.onClick ==> onSubmit(redirectOnSuccess = false),
                    i18n("app.submit-and-create"),
                  )
                },
              )
            ),
          ),
        ),
      )
    }

    private def attachmentDiv(
        filename: String,
        fileSizeBytes: Int,
        attachment: Attachment = null,
    ): VdomTag = {
      <.div(
        ^.key := s"$filename/$fileSizeBytes/${Option(attachment).hashCode()}",
        ^.className := "attachment",
        (if (attachment == null) {
           EmptyVdom
         } else {
           <.a(
             ^.href := AttachmentFormatting.getUrl(attachment),
             ^.target := "_blank",
           )
         }).apply(
          Bootstrap.Glyphicon("paperclip"),
          <.label(s"$filename (${AttachmentFormatting.formatBytes(fileSizeBytes)})"),
        ),
        if (attachment == null) {
          Bootstrap.FontAwesomeIcon("circle-o-notch", "spin")
        } else {
          Bootstrap.Button(Variant.default, Size.xs)(
            ^.onClick --> $.modState(state =>
              state.copy(attachments = state.attachments.filter(_ != attachment))
            ),
            Bootstrap.FontAwesomeIcon("trash-o"),
          )
        },
      )
    }

    private def onPasteEvent(event: ClipboardEvent): Unit = {
      val clipboardData = event.clipboardData
      for (i <- 0 until clipboardData.files.length) {
        event.preventDefault() // Prevent default if there is at least one pasted file

        val file = clipboardData.files(i)

        val attachmentPendingUpload = State.AttachmentPendingUpload(
          filename = file.name,
          fileType = file.`type`,
          fileSizeBytes = file.size.toInt,
        )

        if (! $.state.runNow().containsAttachment(attachmentPendingUpload)) {
          $.modState(state =>
            state.copy(attachmentsPendingUpload = state.attachmentsPendingUpload :+ attachmentPendingUpload)
          ).runNow()

          val fileReader = new FileReader()
          fileReader.onload = e => {
            val result = fileReader.result.asInstanceOf[ArrayBuffer]
            attachmentStore.storeFileAndReturnHash(result).onComplete {
              case Success(hash) =>
                $.modState(state =>
                  state.copy(
                    attachmentsPendingUpload =
                      state.attachmentsPendingUpload.filter(_ != attachmentPendingUpload),
                    attachments = state.attachments :+ attachmentPendingUpload.toAttachment(hash),
                  )
                ).runNow()

              case Failure(exception) =>
                $.modState(state =>
                  state.copy(
                    globalErrorMessage = Some(s"Failed to upload attachment: ${file.name}"),
                    attachmentsPendingUpload =
                      state.attachmentsPendingUpload.filter(_ != attachmentPendingUpload),
                  )
                ).runNow()
            }
          }
          fileReader.readAsArrayBuffer(file)
        }
      }
    }

    private def panelRef(panelIndex: Int): transactionPanel.Reference = {
      while (panelIndex >= _panelRefs.size) {
        _panelRefs += transactionPanel.ref()
      }
      _panelRefs(panelIndex)
    }

    private val addTransactionPanelCallback: Callback = LogExceptionsCallback {
      $.modState(state => logExceptions(state.plusPanel(panelRef))).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(state => logExceptions(state.minusPanelIndex(index, panelRef))).runNow()
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
        }
      ).runNow()
    }

    def onFormChange(): Unit = {
      $.modState(state =>
        logExceptions {
          val flows = for (panelIndex <- state.panelIndices) yield {
            val datedMoney = panelRef(panelIndex).apply().flowValueOrDefault
            datedMoney.exchangedForReferenceCurrency()
          }
          val currencies = for (panelIndex <- state.panelIndices) yield {
            panelRef(panelIndex).apply().moneyReservoir.valueOrDefault.currency
          }

          var newState = state
            .copy(
              foreignCurrency = currencies.find(_.isForeign),
              totalFlowExceptLast = flows.dropRight(1).sum,
            )
            .withRefreshedFlowFractions(panelRef)
          if (state.totalFlowRestriction == TotalFlowRestriction.AnyTotal) {
            newState = newState.copy(totalFlow = flows.sum)
          }
          newState
        }
      ).runNow()
    }

    private def onSubmit(redirectOnSuccess: Boolean)(e: ReactEventFromInput): Callback =
      LogExceptionsCallback {
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
              yield Transaction(
                transactionGroupId = group.id,
                issuerId = issuerId getOrElse user.id,
                beneficiaryAccountCode = data.beneficiaryAccount.code,
                moneyReservoirCode = data.moneyReservoir.code,
                categoryCode = data.category.code,
                description = data.description,
                flowInCents = data.flow.cents,
                detailDescription = data.detailDescription,
                tags = data.tags,
                attachments = state.attachments,
                createdDate = group.createdDate,
                transactionDate = data.transactionDate,
                consumedDate = data.consumedDate,
              )
          }

          val action = props.operationMeta match {
            case OperationMeta.AddNew =>
              AppActions.AddTransactionGroup(transactionsWithoutIdProvider = transactionsWithoutIdProvider(_))
            case OperationMeta.Edit(group, transactions) =>
              AppActions.UpdateTransactionGroup(
                transactionGroupWithId = group,
                transactionsWithoutId = transactionsWithoutIdProvider(group, Some(transactions.head.issuerId)),
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
                  if (redirectOnSuccess) {
                    props.router.setPage(PopupEditorPage.getParentPage(props.router))
                  }
              }
            }

            newState
          }
        ).runNow()
      }

    private def onDelete: Callback = LogExceptionsCallback {
      val props = $.props.runNow()

      props.operationMeta match {
        case OperationMeta.AddNew => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(group, transactions) =>
          dispatcher.dispatch(AppActions.RemoveTransactionGroup(transactionGroupWithId = group))
          props.router.setPage(PopupEditorPage.getParentPage(props.router))
      }
    }
    private def onCopy(implicit router: RouterContext): Callback = LogExceptionsCallback {
      val props = $.props.runNow()

      props.operationMeta match {
        case OperationMeta.AddNew => throw new AssertionError("Should never happen")
        case OperationMeta.Edit(group, transactions) =>
          props.router.setPage(AppPages.NewTransactionGroupFromCopy(transactionGroupId = group.id))
      }
    }
  }

  // **************** Private inner types ****************//
  protected sealed trait OperationMeta
  protected object OperationMeta {
    case object AddNew extends OperationMeta
    case class Edit(group: TransactionGroup, transactions: Seq[Transaction]) extends OperationMeta
  }
}
