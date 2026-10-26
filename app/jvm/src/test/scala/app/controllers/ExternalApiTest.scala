package app.controllers

import java.nio.charset.StandardCharsets
import java.time.Month.MARCH
import java.util.Base64

import app.api.Picklers._
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.accounting.config.Config
import boopickle.Default._
import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Module
import hydro.common.testing.HookedSpecification
import hydro.common.time.LocalDateTimes
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.i18n.MessagesApi
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import play.api.test.WithApplication

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ExternalApiTest extends HookedSpecification {

  @Inject implicit private val accountingConfig: Config = null
  @Inject private val externalApi: ExternalApi = null

  override def before() = {
    val injector = Guice.createInjector(
      new TestModule,
      new Module {
        override def configure(binder: Binder): Unit = {
          binder.bind(classOf[ControllerComponents]).toInstance(Helpers.stubControllerComponents())
          binder.bind(classOf[MessagesApi]).toInstance(Helpers.stubMessagesApi())
        }
      },
    )
    injector.injectMembers(this)
  }

  "interactiveTransactionsPreload" in new WithApplication {
    val json =
      s"""{
         |  "transactions": [
         |    {
         |      "beneficiaryCode": "${testAccountA.code}",
         |      "moneyReservoirCode": "${testReservoirCashA.code}",
         |      "categoryCode": "${testCategoryA.code}",
         |      "description": "Groceries",
         |      "flowAsFloat": -12.34,
         |      "detailDescription": "Some details",
         |      "tags": ["tag1", "tag2"],
         |      "transactionDate": "2026-03-15",
         |      "consumedDate": "2026-03-16"
         |    },
         |    {
         |      "description": "Minimal transaction"
         |    }
         |  ]
         |}""".stripMargin
    val base64Json = Base64.getEncoder.encodeToString(json.getBytes(StandardCharsets.UTF_8))

    val result = externalApi.interactiveTransactionsPreload(s"base64:$base64Json")(FakeRequest())

    status(result) mustEqual SEE_OTHER
    val location = redirectLocation(result).get
    val prefix = "/app/@newtransactiongroupfrompreload/"
    val suffix = "@/cashflow"
    location must startWith(prefix)
    location must endWith(suffix)

    val encodedTransactionGroup = location.stripPrefix(prefix).stripSuffix(suffix)
    val decodedTransactionGroup = unpickleFromUrlSafeBase64[TransactionGroup.Partial](encodedTransactionGroup)
    decodedTransactionGroup mustEqual TransactionGroup.Partial(
      transactions = Seq(
        Transaction.Partial(
          beneficiary = Some(testAccountA),
          moneyReservoir = Some(testReservoirCashA),
          category = Some(testCategoryA),
          description = "Groceries",
          flowInCents = -1234,
          detailDescription = "Some details",
          tags = Seq("tag1", "tag2"),
          transactionDate = Some(LocalDateTimes.createDateTime(2026, MARCH, 15)),
          consumedDate = Some(LocalDateTimes.createDateTime(2026, MARCH, 16)),
        ),
        Transaction.Partial(
          description = "Minimal transaction",
        ),
      ),
      zeroSum = false,
    )
  }
}
