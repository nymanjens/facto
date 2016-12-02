
package controllers.accounting

import scala.collection.JavaConverters._
import com.google.common.base.Joiner
import com.google.inject.Inject
import play.api.mvc.{AnyContent, Controller, Flash, Request, Result}
import common.{GetParameter, I18n}
import common.time.{Clock, TimeUtils}
import common.CollectionUtils.toListMap
import models._
import models.accounting.{Tag, TagEntity}
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager
import models.accounting.config.{Account, Category, MoneyReservoir, Template}
import controllers.helpers.AuthenticatedAction
import controllers.helpers.accounting._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

final class Views @Inject()(implicit val messagesApi: MessagesApi,
                            clock: Clock,
                            accountingConfig: Config,
                            generalEntries: GeneralEntries,
                            cashFlowEntries: CashFlowEntries,
                            liquidationEntries: LiquidationEntries,
                            summaries: Summaries,
                            tagEntityManager: TagEntity.Manager,
                            exchangeRateManager: ExchangeRateManager,
                            entityAccess: EntityAccess,
                            i18n: I18n) extends Controller with I18nSupport {

  // ********** actions - views ********** //
  def everythingLatest = AuthenticatedAction { implicit user =>
    implicit request =>
      everything(numEntriesToShow = 400)
  }

  def everythingAll = AuthenticatedAction { implicit user =>
    implicit request =>
      everything()
  }

  def cashFlowOfAll = AuthenticatedAction { implicit user =>
    implicit request =>
      cashFlow(
        reservoirs = accountingConfig.visibleReservoirs,
        numEntriesShownByDefaultToShow = 10,
        expandedNumEntriesToShow = 30)
  }

  def cashFlowOfSingle(reservoirCode: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      cashFlow(
        reservoirs = Seq(accountingConfig.moneyReservoir(reservoirCode)))
  }

  def cashFlowOfHidden = AuthenticatedAction { implicit user =>
    implicit request =>
      cashFlow(
        reservoirs = accountingConfig.moneyReservoirs(includeHidden = true).filter(_.hidden),
        numEntriesShownByDefaultToShow = 10,
        expandedNumEntriesToShow = 30)
  }

  def liquidationOfAll = AuthenticatedAction { implicit user =>
    implicit request =>
      val allCombinations: Seq[AccountPair] =
        for {
          (acc1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
          (acc2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
          if i1 < i2
        } yield AccountPair(acc1, acc2)
      liquidation(
        accountPairs = allCombinations,
        numEntriesShownByDefaultToShow = 10,
        expandedNumEntriesToShow = 30)
  }

  def liquidationOfSingle(accountCode1: String, accountCode2: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      liquidation(
        accountPairs = Seq(AccountPair(accountingConfig.accounts(accountCode1), accountingConfig.accounts(accountCode2))))
  }

  def endowmentsOfAll = AuthenticatedAction { implicit user =>
    implicit request =>
      endowments(
        accounts = accountingConfig.personallySortedAccounts,
        numEntriesShownByDefaultToShow = 30,
        expandedNumEntriesToShow = 100)
  }

  def endowmentsOfSingle(accountCode: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      endowments(
        accounts = Seq(accountingConfig.accounts(accountCode)))
  }

  def summaryForCurrentYear(tags: String = "", toggleTag: String = "") = AuthenticatedAction { implicit user =>
    implicit request =>
      summary(
        accounts = accountingConfig.personallySortedAccounts,
        expandedYear = clock.now.getYear,
        tagsString = tags,
        toggleTag = toggleTag)
  }

  def summaryFor(expandedYear: Int, tags: String, toggleTag: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      summary(
        accounts = accountingConfig.personallySortedAccounts,
        expandedYear,
        tagsString = tags,
        toggleTag = toggleTag)
  }

  // ********** private helper controllers ********** //
  private def everything(numEntriesToShow: Int = 100000)(implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val entries = generalEntries.fetchLastNEntries(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.everything(
      entries = entries,
      numEntriesToShow = numEntriesToShow,
      templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.EverythingView, user)))
  }

  private def cashFlow(reservoirs: Iterable[MoneyReservoir],
                       numEntriesShownByDefaultToShow: Int = 100000,
                       expandedNumEntriesToShow: Int = 100000)
                      (implicit request: Request[AnyContent], user: User): Result = {
    // get reservoirToEntries
    val reservoirToEntries = toListMap {
      for (res <- reservoirs) yield {
        res -> cashFlowEntries.fetchLastNEntries(moneyReservoir = res, n = expandedNumEntriesToShow + 1)
      }
    }

    // get sorted accounts -> reservoir to show
    val accountToReservoirs = reservoirs.groupBy(_.owner)
    val sortedAccountToReservoirs = toListMap(
      for (acc <- accountingConfig.personallySortedAccounts; if accountToReservoirs.contains(acc))
        yield (acc, accountToReservoirs(acc)))

    // render
    Ok(views.html.accounting.cashflow(
      accountToReservoirs = sortedAccountToReservoirs,
      reservoirToEntries = reservoirToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.CashFlowView, user)))
  }


  private def liquidation(accountPairs: Seq[AccountPair],
                          numEntriesShownByDefaultToShow: Int = 100000,
                          expandedNumEntriesToShow: Int = 100000)
                         (implicit request: Request[AnyContent], user: User): Result = {
    // get pairsToEntries
    val pairsToEntries = toListMap(
      for (accountPair <- accountPairs)
        yield (accountPair, liquidationEntries.fetchLastNEntries(accountPair, n = expandedNumEntriesToShow + 1)))

    // render
    Ok(views.html.accounting.liquidation(
      pairsToEntries = pairsToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.LiquidationView, user)))
  }

  private def endowments(accounts: Iterable[Account],
                         numEntriesShownByDefaultToShow: Int = 100000,
                         expandedNumEntriesToShow: Int = 100000)
                        (implicit request: Request[AnyContent], user: User): Result = {
    // get accountToEntries
    val accountToEntries = toListMap {
      for (account <- accounts) yield account -> generalEntries.fetchLastNEndowments(account, n = expandedNumEntriesToShow + 1)
    }

    // render
    Ok(views.html.accounting.endowments(
      accountToEntries = accountToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.EndowmentsView, user)))
  }

  private def summary(accounts: Iterable[Account], expandedYear: Int, tagsString: String, toggleTag: String)
                     (implicit request: Request[AnyContent], user: User): Result = {
    val tags = Tag.parseTagsString(tagsString)

    if (toggleTag != "") {
      // Redirect to the same page with the toggled tag in tagsString
      val newTags = {
        val newTag = Tag(toggleTag)
        if (tags contains newTag) {
          tags.filter(_ != newTag)
        } else if (Tag.isValidTagName(toggleTag)) {
          tags ++ Seq(newTag)
        } else {
          tags
        }
      }
      val newTagsString = Joiner.on(",").join(newTags.map(_.name).asJava)
      Redirect(controllers.accounting.routes.Views.summaryFor(expandedYear, newTagsString, toggleTag = ""))

    } else {
      // get accountToEntries
      val accountToSummary = toListMap {
        for (account <- accounts) yield account -> summaries.fetchSummary(account, expandedYear, tags)
      }

      // render
      Ok(views.html.accounting.summary(
        accountToSummary,
        expandedYear,
        templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.SummaryView, user),
        tags = tags,
        tagsString = tagsString))
    }
  }
}
