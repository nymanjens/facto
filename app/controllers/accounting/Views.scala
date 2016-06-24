package controllers.accounting

import play.api.mvc.{Controller, Flash, Result, Request, AnyContent}

import common.Clock
import common.CollectionUtils.toListMap
import models.User
import models.accounting.config.Config
import models.accounting.config.{Account, MoneyReservoir, Category, Template}
import controllers.helpers.AuthenticatedAction
import controllers.helpers.accounting._

object Views extends Controller {

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
        reservoirs = Config.visibleReservoirs,
        numEntriesShownByDefaultToShow = 10,
        expandedNumEntriesToShow = 30)
  }

  def cashFlowOfSingle(reservoirCode: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      cashFlow(
        reservoirs = Seq(Config.moneyReservoir(reservoirCode)))
  }

  def liquidationOfAll = AuthenticatedAction { implicit user =>
    implicit request =>
      val allCombinations: Seq[AccountPair] =
        for {
          (acc1, i1) <- Config.personallySortedAccounts.zipWithIndex
          (acc2, i2) <- Config.personallySortedAccounts.zipWithIndex
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
        accountPairs = Seq(AccountPair(Config.accounts(accountCode1), Config.accounts(accountCode2))))
  }

  def endowmentsOfAll = AuthenticatedAction { implicit user =>
    implicit request =>
      endowments(
        accounts = Config.personallySortedAccounts,
        numEntriesShownByDefaultToShow = 30,
        expandedNumEntriesToShow = 100)
  }

  def endowmentsOfSingle(accountCode: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      endowments(
        accounts = Seq(Config.accounts(accountCode)))
  }

  def summaryForCurrentYear = AuthenticatedAction { implicit user =>
    implicit request =>
      summary(
        accounts = Config.personallySortedAccounts,
        expandedYear = Clock.now.getYear)
  }

  def summaryFor(expandedYear: Int) = AuthenticatedAction { implicit user =>
    implicit request =>
      summary(
        accounts = Config.personallySortedAccounts,
        expandedYear)
  }

  // ********** private helper controllers ********** //
  private def everything(numEntriesToShow: Int = 100000)(implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val entries = GeneralEntry.fetchLastNEntries(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.everything(
      entries = entries,
      numEntriesToShow = numEntriesToShow,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.EverythingView, user)))
  }

  private def cashFlow(reservoirs: Iterable[MoneyReservoir],
                       numEntriesShownByDefaultToShow: Int = 100000,
                       expandedNumEntriesToShow: Int = 100000)
                      (implicit request: Request[AnyContent], user: User): Result = {
    // get reservoirToEntries
    val reservoirToEntries = toListMap {
      for (res <- reservoirs) yield {
        res -> CashFlowEntry.fetchLastNEntries(moneyReservoir = res, n = expandedNumEntriesToShow + 1)
      }
    }

    // get sorted accounts -> reservoir to show
    val accountToReservoirs = reservoirs.groupBy(_.owner)
    val sortedAccountToReservoirs = toListMap(
      for (acc <- Config.personallySortedAccounts; if accountToReservoirs.contains(acc))
        yield (acc, accountToReservoirs(acc)))

    // render
    Ok(views.html.accounting.cashflow(
      accountToReservoirs = sortedAccountToReservoirs,
      reservoirToEntries = reservoirToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.CashFlowView, user)))
  }


  private def liquidation(accountPairs: Seq[AccountPair],
                          numEntriesShownByDefaultToShow: Int = 100000,
                          expandedNumEntriesToShow: Int = 100000)
                         (implicit request: Request[AnyContent], user: User): Result = {
    // get pairsToEntries
    val pairsToEntries = toListMap(
      for (accountPair <- accountPairs)
        yield (accountPair, LiquidationEntry.fetchLastNEntries(accountPair, n = expandedNumEntriesToShow + 1)))

    // render
    Ok(views.html.accounting.liquidation(
      pairsToEntries = pairsToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.LiquidationView, user)))
  }

  private def endowments(accounts: Iterable[Account],
                         numEntriesShownByDefaultToShow: Int = 100000,
                         expandedNumEntriesToShow: Int = 100000)
                        (implicit request: Request[AnyContent], user: User): Result = {
    // get accountToEntries
    val accountToEntries = toListMap {
      for (account <- accounts) yield account -> GeneralEntry.fetchLastNEndowments(account, n = expandedNumEntriesToShow + 1)
    }

    // render
    Ok(views.html.accounting.endowments(
      accountToEntries = accountToEntries,
      numEntriesShownByDefault = numEntriesShownByDefaultToShow,
      expandedNumEntries = expandedNumEntriesToShow,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.EndowmentsView, user)))
  }

  private def summary(accounts: Iterable[Account], expandedYear: Int)
                     (implicit request: Request[AnyContent], user: User): Result = {
    // get accountToEntries
    val accountToSummary = toListMap {
      for (account <- accounts) yield account -> Summary.fetchSummary(account, expandedYear)
    }

    // render
    Ok(views.html.accounting.summary(
      accountToSummary,
      expandedYear,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.SummaryView, user)))
  }
}
