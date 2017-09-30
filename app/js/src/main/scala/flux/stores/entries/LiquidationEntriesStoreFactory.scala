package flux.stores.entries

import common.LoggingUtils.logExceptions
import common.GuavaReplacement.Stopwatch
import jsfacades.LokiJs
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.accounting.config.{Account, Config, MoneyReservoir}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq
import scala2js.Keys
import scala2js.Converters._

final class LiquidationEntriesStoreFactory(implicit database: RemoteDatabaseProxy,
                                           accountingConfig: Config,
                                           exchangeRateManager: ExchangeRateManager,
                                           entityAccess: EntityAccess)
    extends EntriesListStoreFactory[LiquidationEntry, AccountPair] {

  override protected def createNew(maxNumEntries: Int, accountPair: AccountPair) =
    new TransactionsListStore[LiquidationEntry] {
      override protected def calculateState() = logExceptions {
        val stopwatch = Stopwatch.createStarted()
        val result = calculatePart2(getTransactions1_original())
//        require(calculatePart2(getTransactions2_multipleQueries()) == result)
        require(calculatePart2(getTransactions3_complexQuery()) == result)

        def time(name: String)(func: => Seq[Transaction]): Unit = {
          println(s"  Calculating for $accountPair ($name)")
          val stopwatch = Stopwatch.createStarted()
          for (_ <- 1 to 10) {
            val result = func
            println(s"Got ${result.length} rows")
          }
          println(s"  Done in ${stopwatch.elapsed.toMillis / 1e3} seconds")
        }

        for (_ <- 1 to 2) {
          time("1 - original")(getTransactions1_original())
          time("2 - different requests")(getTransactions2_multipleQueries())
          time("3 - single complex filter")(getTransactions3_complexQuery())
          println("")
        }
          println("")

        result
      }

      private def calculatePart2(allTransactions: Seq[Transaction]) = {
        val relevantTransactions =
          for {
            transaction <- allTransactions
            if isRelevantForAccounts(transaction, accountPair)
          } yield transaction

        // convert to entries (recursion does not lead to growing stack because of Stream)
        def convertToEntries(nextTransactions: List[Transaction],
                             currentDebt: ReferenceMoney): Stream[LiquidationEntry] =
          nextTransactions match {
            case trans :: rest =>
              val addsTo1To2Debt = trans.beneficiary == accountPair.account2
              val flow = trans.flow.exchangedForReferenceCurrency
              val newDebt = if (addsTo1To2Debt) currentDebt + flow else currentDebt - flow
              LiquidationEntry(Seq(trans), newDebt) #:: convertToEntries(rest, newDebt)
            case Nil =>
              Stream.empty
          }
        var entries =
          convertToEntries(relevantTransactions.toList, ReferenceMoney(0) /* initial debt */ ).toList

        entries = GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
          /* combine */
          (first, last) =>
            LiquidationEntry(first.transactions ++ last.transactions, last.debt)
        }

        EntriesListStoreFactory.State(
          entries.takeRight(maxNumEntries),
          hasMore = entries.size > maxNumEntries)
      }

      private def getTransactions1_original() = {
        database
          .newQuery[Transaction]()
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.Transaction.transactionDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()
      }

      private def getTransactions2_multipleQueries() = {
        def dataWithFilter(
            filter: LokiJs.ResultSet[Transaction] => LokiJs.ResultSet[Transaction]): Seq[Transaction] = {
          filter(database.newQuery[Transaction]())
            .sort(
              LokiJs.Sorting
                .ascBy(Keys.Transaction.transactionDate)
                .thenAscBy(Keys.Transaction.createdDate)
                .thenAscBy(Keys.id))
            .data()
        }
        def reservoirsOwnedBy(account: Account): Seq[MoneyReservoir] = {
          accountingConfig.moneyReservoirs(includeHidden = true).filter(r => r.owner == account)
        }

        Seq(
          dataWithFilter(
            _.filterAnyOf(
              Keys.Transaction.moneyReservoirCode,
              reservoirsOwnedBy(accountPair.account1).map(_.code))
              .filter(Keys.Transaction.beneficiaryAccountCode, accountPair.account2.code)),
          dataWithFilter(
            _.filterAnyOf(
              Keys.Transaction.moneyReservoirCode,
              reservoirsOwnedBy(accountPair.account2).map(_.code))
              .filter(Keys.Transaction.beneficiaryAccountCode, accountPair.account1.code)),
          dataWithFilter(
            _.filter(Keys.Transaction.moneyReservoirCode, "")
              .filterAnyOf(Keys.Transaction.beneficiaryAccountCode, accountPair.toSet.map(_.code).toVector))
        ).flatten
      }

      private def getTransactions3_complexQuery() = {
        def reservoirsOwnedBy(account: Account): Seq[MoneyReservoir] = {
          accountingConfig.moneyReservoirs(includeHidden = true).filter(r => r.owner == account)
        }

        database
          .newQuery[Transaction]()
          .filter(LokiJs.ResultSet.Filter.or(
            LokiJs.ResultSet.Filter.and(
              LokiJs.ResultSet.Filter.anyOf(
                Keys.Transaction.moneyReservoirCode,
                reservoirsOwnedBy(accountPair.account1).map(_.code)),
              LokiJs.ResultSet.Filter
                .equal(Keys.Transaction.beneficiaryAccountCode, accountPair.account2.code)
            ),
            LokiJs.ResultSet.Filter.and(
              LokiJs.ResultSet.Filter.anyOf(
                Keys.Transaction.moneyReservoirCode,
                reservoirsOwnedBy(accountPair.account2).map(_.code)),
              LokiJs.ResultSet.Filter
                .equal(Keys.Transaction.beneficiaryAccountCode, accountPair.account1.code)
            ),
            LokiJs.ResultSet.Filter.and(
              LokiJs.ResultSet.Filter.equal(Keys.Transaction.moneyReservoirCode, ""),
              LokiJs.ResultSet.Filter
                .anyOf(Keys.Transaction.beneficiaryAccountCode, accountPair.toSet.map(_.code).toVector)
            )
          ))
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.Transaction.transactionDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()
      }

      override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
        isRelevantForAccounts(transaction, accountPair)

      private def isRelevantForAccounts(transaction: Transaction, accountPair: AccountPair): Boolean = {
        val moneyReservoirOwner = transaction.moneyReservoirCode match {
          case "" =>
            // Pick the first beneficiary in the group. This simulates that the zero sum transaction was physically
            // performed on an actual reservoir, which is needed for the liquidation calculator to work.
            transaction.transactionGroup.transactions.head.beneficiary
          case _ => transaction.moneyReservoir.owner
        }
        val involvedAccounts: Set[Account] = Set(transaction.beneficiary, moneyReservoirOwner)
        accountPair.toSet == involvedAccounts
      }
    }

  def get(accountPair: AccountPair, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = accountPair))
}
