package flux.stores.entries

import java.time.Duration
import java.time.Month.JANUARY

import common.money.{Currency, MoneyWithGeneralCurrency}
import common.testing.TestObjects._
import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.time.LocalDateTimes.createDateTime
import flux.stores.entries.CashFlowEntry.{BalanceCorrection, RegularEntry}
import models.accounting._
import models.accounting.config.MoneyReservoir
import common.money.Currency
import models.modification.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

// Note: Testing with GeneralEntry because GroupedTransactions is abstract
object GroupedTransactionsTest extends TestSuite {

  override def tests = TestSuite {
    "description()" - {
      "single transaction" - {
        createGroupedTransactions("abc def: ghi").description ==> "abc def: ghi"
      }

      "multiple transactions" - {
        "No matching prefix" - {
          createGroupedTransactions("abc", "abd").description ==> "abc, abd"
          createGroupedTransactions("abc", "xyz").description ==> "abc, xyz"
        }
        "Filter duplicates" - {
          createGroupedTransactions("abc", "abc").description ==> "abc"
        }
        "Matching prefix ending with colon" - {
          createGroupedTransactions("abc:def", "abc:ghi").description ==> "abc:{def, ghi}"
        }
        "Matching prefix ending with space" - {
          createGroupedTransactions("abc def", "abc ghi").description ==> "abc {def, ghi}"
        }
        "Matching prefix ending with colon and space" - {
          createGroupedTransactions("abc: def", "abc: ghi").description ==> "abc: {def, ghi}"
        }
        "Matching prefix with empty suffix" - {
          createGroupedTransactions("abc", "abc: ghi", "abc: jkl").description ==> "abc{: ghi, : jkl}"
        }
      }
    }
  }

  private def createGroupedTransactions(descriptions: String*): GroupedTransactions = {
    GeneralEntry(
      descriptions
        .map(description => createTransaction(groupId = 18282, description = description))
        .toVector)
  }
}
