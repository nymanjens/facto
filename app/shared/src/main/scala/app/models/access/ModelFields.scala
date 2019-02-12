package app.models.access

import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.models.modification.EntityType
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.common.CollectionUtils
import hydro.common.ScalaUtils
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.models.access.ModelField.IdModelField

import scala.collection.immutable.Seq

object ModelFields {

  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case app.models.user.User.Type                   => User.id.asInstanceOf[ModelField[Long, E]]
    case app.models.accounting.Transaction.Type      => Transaction.id.asInstanceOf[ModelField[Long, E]]
    case app.models.accounting.TransactionGroup.Type => TransactionGroup.id.asInstanceOf[ModelField[Long, E]]
    case app.models.accounting.BalanceCheck.Type     => BalanceCheck.id.asInstanceOf[ModelField[Long, E]]
    case app.models.money.ExchangeRateMeasurement.Type =>
      ExchangeRateMeasurement.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Enumeration of all fields **************** //
  object User {
    private type E = User

    case object id extends IdModelField[E]
    case object loginName extends ModelField[String, E]("loginName", _.loginName, v => _.copy(loginName = v))
    case object passwordHash
        extends ModelField[String, E]("passwordHash", _.passwordHash, v => _.copy(passwordHash = v))
    case object name extends ModelField[String, E]("name", _.name, v => _.copy(name = v))
    case object isAdmin extends ModelField[Boolean, E]("isAdmin", _.isAdmin, v => _.copy(isAdmin = v))
    case object expandCashFlowTablesByDefault
        extends ModelField[Boolean, E](
          "expandCashFlowTablesByDefault",
          _.expandCashFlowTablesByDefault,
          v => _.copy(expandCashFlowTablesByDefault = v))
    case object expandLiquidationTablesByDefault
        extends ModelField[Boolean, E](
          "expandLiquidationTablesByDefault",
          _.expandLiquidationTablesByDefault,
          v => _.copy(expandLiquidationTablesByDefault = v))
  }

  object Transaction {
    private type E = app.models.accounting.Transaction

    case object id extends IdModelField[E]
    case object transactionGroupId
        extends ModelField[Long, E](
          "transactionGroupId",
          _.transactionGroupId,
          v => _.copy(transactionGroupId = v))
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId, v => _.copy(issuerId = v))
    case object beneficiaryAccountCode
        extends ModelField[String, E](
          "beneficiaryAccountCode",
          _.beneficiaryAccountCode,
          v => _.copy(beneficiaryAccountCode = v))
    case object moneyReservoirCode
        extends ModelField[String, E](
          "moneyReservoirCode",
          _.moneyReservoirCode,
          v => _.copy(moneyReservoirCode = v))
    case object categoryCode
        extends ModelField[String, E]("categoryCode", _.categoryCode, v => _.copy(categoryCode = v))
    case object description
        extends ModelField[String, E]("description", _.description, v => _.copy(description = v))
    case object flowInCents
        extends ModelField[Long, E]("flowInCents", _.flowInCents, v => _.copy(flowInCents = v))
    case object detailDescription
        extends ModelField[String, E](
          "detailDescription",
          _.detailDescription,
          v => _.copy(detailDescription = v))
    case object tags extends ModelField[Seq[String], E]("tags", _.tags, v => _.copy(tags = v))
    case object createdDate
        extends ModelField[LocalDateTime, E]("createdDate", _.createdDate, v => _.copy(createdDate = v))
    case object transactionDate
        extends ModelField[LocalDateTime, E](
          "transactionDate",
          _.transactionDate,
          v => _.copy(transactionDate = v))
    case object consumedDate
        extends ModelField[LocalDateTime, E]("consumedDate", _.consumedDate, v => _.copy(consumedDate = v))
  }

  object TransactionGroup {
    private type E = app.models.accounting.TransactionGroup

    case object id extends IdModelField[E]
    case object createdDate
        extends ModelField[LocalDateTime, E]("createdDate", _.createdDate, v => _.copy(createdDate = v))
  }

  object BalanceCheck {
    private type E = app.models.accounting.BalanceCheck

    case object id extends IdModelField[E]
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId, v => _.copy(issuerId = v))
    case object moneyReservoirCode
        extends ModelField[String, E](
          "moneyReservoirCode",
          _.moneyReservoirCode,
          v => _.copy(moneyReservoirCode = v))
    case object balanceInCents
        extends ModelField[Long, E]("balanceInCents", _.balanceInCents, v => _.copy(balanceInCents = v))
    case object createdDate
        extends ModelField[LocalDateTime, E]("createdDate", _.createdDate, v => _.copy(createdDate = v))
    case object checkDate
        extends ModelField[LocalDateTime, E]("checkDate", _.checkDate, v => _.copy(checkDate = v))
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    case object id extends IdModelField[E]
    case object date extends ModelField[LocalDateTime, E]("date", _.date, v => _.copy(date = v))
    case object foreignCurrencyCode
        extends ModelField[String, E](
          "foreignCurrencyCode",
          _.foreignCurrencyCode,
          v => _.copy(foreignCurrencyCode = v))
    case object ratioReferenceToForeignCurrency
        extends ModelField[Double, E](
          "ratioReferenceToForeignCurrency",
          _.ratioReferenceToForeignCurrency,
          v => _.copy(ratioReferenceToForeignCurrency = v))
  }

  // **************** Field numbers **************** //
  private val fieldToNumberMap: ImmutableBiMap[ModelField.any, Int] =
    CollectionUtils.toBiMapWithStableIntKeys(
      stableNameMapper = field =>
        ScalaUtils.stripRequiredPrefix(field.getClass.getName, prefix = ModelFields.getClass.getName),
      values = Seq(
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
    )

  def toNumber(field: ModelField.any): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField.any = fieldToNumberMap.inverse().get(number)
}
