package app.models.modification

import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

object EntityTypes {

  lazy val all: Seq[EntityType.any] =
    Seq(User.Type, Transaction.Type, TransactionGroup.Type, BalanceCheck.Type, ExchangeRateMeasurement.Type)
}
