package app.models.access

import app.common.GuavaReplacement.ImmutableBiMap
import hydro.models.Entity
import hydro.models.access.ModelField.IdModelField
import hydro.models.access.ModelField.toBiMapWithUniqueValues
import app.models.modification.EntityType
import app.models.modification.EntityType._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.models.access.ModelField

import scala.collection.immutable.Seq

object ModelFields {

  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case app.models.user.User.Type   => User.id.asInstanceOf[ModelField[Long, E]]
    case TransactionType             => Transaction.id.asInstanceOf[ModelField[Long, E]]
    case TransactionGroupType        => TransactionGroup.id.asInstanceOf[ModelField[Long, E]]
    case BalanceCheckType            => BalanceCheck.id.asInstanceOf[ModelField[Long, E]]
    case ExchangeRateMeasurementType => ExchangeRateMeasurement.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Enumeration of all fields **************** //
  object User {
    private type E = User

    case object id extends IdModelField[E]
    case object loginName extends ModelField[String, E]("loginName", _.loginName)
    case object passwordHash extends ModelField[String, E]("passwordHash", _.passwordHash)
    case object name extends ModelField[String, E]("name", _.name)
    case object isAdmin extends ModelField[Boolean, E]("isAdmin", _.isAdmin)
    case object expandCashFlowTablesByDefault
        extends ModelField[Boolean, E]("expandCashFlowTablesByDefault", _.expandCashFlowTablesByDefault)
    case object expandLiquidationTablesByDefault
        extends ModelField[Boolean, E]("expandLiquidationTablesByDefault", _.expandLiquidationTablesByDefault)
  }

  object Transaction {
    private type E = app.models.accounting.Transaction

    case object id extends IdModelField[E]
    case object transactionGroupId extends ModelField[Long, E]("transactionGroupId", _.transactionGroupId)
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId)
    case object beneficiaryAccountCode
        extends ModelField[String, E]("beneficiaryAccountCode", _.beneficiaryAccountCode)
    case object moneyReservoirCode extends ModelField[String, E]("moneyReservoirCode", _.moneyReservoirCode)
    case object categoryCode extends ModelField[String, E]("categoryCode", _.categoryCode)
    case object description extends ModelField[String, E]("description", _.description)
    case object flowInCents extends ModelField[Long, E]("flowInCents", _.flowInCents)
    case object detailDescription extends ModelField[String, E]("detailDescription", _.detailDescription)
    case object tags extends ModelField[Seq[String], E]("tags", _.tags)
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
    case object transactionDate extends ModelField[LocalDateTime, E]("transactionDate", _.transactionDate)
    case object consumedDate extends ModelField[LocalDateTime, E]("consumedDate", _.consumedDate)
  }

  object TransactionGroup {
    private type E = app.models.accounting.TransactionGroup

    case object id extends IdModelField[E]
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
  }

  object BalanceCheck {
    private type E = app.models.accounting.BalanceCheck

    case object id extends IdModelField[E]
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId)
    case object moneyReservoirCode extends ModelField[String, E]("moneyReservoirCode", _.moneyReservoirCode)
    case object balanceInCents extends ModelField[Long, E]("balanceInCents", _.balanceInCents)
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
    case object checkDate extends ModelField[LocalDateTime, E]("checkDate", _.checkDate)
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    case object id extends IdModelField[E]
    case object date extends ModelField[LocalDateTime, E]("date", _.date)
    case object foreignCurrencyCode
        extends ModelField[String, E]("foreignCurrencyCode", _.foreignCurrencyCode)
    case object ratioReferenceToForeignCurrency
        extends ModelField[Double, E]("ratioReferenceToForeignCurrency", _.ratioReferenceToForeignCurrency)
  }

  // **************** Field numbers **************** //
  private val fieldToNumberMap: ImmutableBiMap[ModelField[_, _], Int] =
    toBiMapWithUniqueValues(
      User.id,
      User.loginName,
      User.passwordHash,
      User.name,
      User.isAdmin,
      User.expandCashFlowTablesByDefault,
      User.expandLiquidationTablesByDefault,
      Transaction.id,
      Transaction.transactionGroupId,
      Transaction.issuerId,
      Transaction.beneficiaryAccountCode,
      Transaction.moneyReservoirCode,
      Transaction.categoryCode,
      Transaction.description,
      Transaction.flowInCents,
      Transaction.detailDescription,
      Transaction.tags,
      Transaction.createdDate,
      Transaction.transactionDate,
      Transaction.consumedDate,
      TransactionGroup.id,
      TransactionGroup.createdDate,
      BalanceCheck.id,
      BalanceCheck.issuerId,
      BalanceCheck.moneyReservoirCode,
      BalanceCheck.balanceInCents,
      BalanceCheck.createdDate,
      BalanceCheck.checkDate,
      ExchangeRateMeasurement.id,
      ExchangeRateMeasurement.date,
      ExchangeRateMeasurement.foreignCurrencyCode,
      ExchangeRateMeasurement.ratioReferenceToForeignCurrency
    )

  def toNumber(field: ModelField[_, _]): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField[_, _] = fieldToNumberMap.inverse().get(number)
}
