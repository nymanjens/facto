package api

import scala.collection.immutable.Seq
import models.accounting.config.Config
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.User
import api.ScalaJsApi.AllEntries

trait ScalaJsApi {

  def getAccountingConfig(): Config

  def getAllEntities(): AllEntries
}

object ScalaJsApi {

  case class AllEntries(users: Seq[User],
                        transactions: Seq[Transaction],
                        transactionGroups: Seq[TransactionGroup],
                        balanceChecks: Seq[BalanceCheck],
                        exchangeRateMeasurements: Seq[ExchangeRateMeasurement])
}
