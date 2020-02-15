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

    val id = ModelField.forId[E]()
    val loginName: ModelField[String, E] = ModelField("loginName", _.loginName, v => _.copy(loginName = v))
    val passwordHash: ModelField[String, E] =
      ModelField("passwordHash", _.passwordHash, v => _.copy(passwordHash = v))
    val name: ModelField[String, E] = ModelField("name", _.name, v => _.copy(name = v))
    val isAdmin: ModelField[Boolean, E] = ModelField("isAdmin", _.isAdmin, v => _.copy(isAdmin = v))
    val expandCashFlowTablesByDefault: ModelField[Boolean, E] = ModelField(
      "expandCashFlowTablesByDefault",
      _.expandCashFlowTablesByDefault,
      v => _.copy(expandCashFlowTablesByDefault = v))
    val expandLiquidationTablesByDefault: ModelField[Boolean, E] = ModelField(
      "expandLiquidationTablesByDefault",
      _.expandLiquidationTablesByDefault,
      v => _.copy(expandLiquidationTablesByDefault = v))
  }

  object Transaction {
    private type E = app.models.accounting.Transaction

    val id = ModelField.forId[E]()
    val transactionGroupId: ModelField[Long, E] =
      ModelField("transactionGroupId", _.transactionGroupId, v => _.copy(transactionGroupId = v))
    val issuerId: ModelField[Long, E] = ModelField("issuerId", _.issuerId, v => _.copy(issuerId = v))
    val beneficiaryAccountCode: ModelField[String, E] =
      ModelField("beneficiaryAccountCode", _.beneficiaryAccountCode, v => _.copy(beneficiaryAccountCode = v))
    val moneyReservoirCode: ModelField[String, E] =
      ModelField("moneyReservoirCode", _.moneyReservoirCode, v => _.copy(moneyReservoirCode = v))
    val categoryCode: ModelField[String, E] =
      ModelField("categoryCode", _.categoryCode, v => _.copy(categoryCode = v))
    val description: ModelField[String, E] =
      ModelField("description", _.description, v => _.copy(description = v))
    val flowInCents: ModelField[Long, E] =
      ModelField("flowInCents", _.flowInCents, v => _.copy(flowInCents = v))
    val detailDescription: ModelField[String, E] =
      ModelField("detailDescription", _.detailDescription, v => _.copy(detailDescription = v))
    val tags: ModelField[Seq[String], E] = ModelField("tags", _.tags, v => _.copy(tags = v))
    val createdDate: ModelField[LocalDateTime, E] =
      ModelField("createdDate", _.createdDate, v => _.copy(createdDate = v))
    val transactionDate: ModelField[LocalDateTime, E] =
      ModelField("transactionDate", _.transactionDate, v => _.copy(transactionDate = v))
    val consumedDate: ModelField[LocalDateTime, E] =
      ModelField("consumedDate", _.consumedDate, v => _.copy(consumedDate = v))
  }

  object TransactionGroup {
    private type E = app.models.accounting.TransactionGroup

    val id = ModelField.forId[E]()
    val createdDate: ModelField[LocalDateTime, E] =
      ModelField("createdDate", _.createdDate, v => _.copy(createdDate = v))
  }

  object BalanceCheck {
    private type E = app.models.accounting.BalanceCheck

    val id = ModelField.forId[E]()
    val issuerId: ModelField[Long, E] = ModelField("issuerId", _.issuerId, v => _.copy(issuerId = v))
    val moneyReservoirCode: ModelField[String, E] =
      ModelField("moneyReservoirCode", _.moneyReservoirCode, v => _.copy(moneyReservoirCode = v))
    val balanceInCents: ModelField[Long, E] =
      ModelField("balanceInCents", _.balanceInCents, v => _.copy(balanceInCents = v))
    val createdDate: ModelField[LocalDateTime, E] =
      ModelField("createdDate", _.createdDate, v => _.copy(createdDate = v))
    val checkDate: ModelField[LocalDateTime, E] =
      ModelField("checkDate", _.checkDate, v => _.copy(checkDate = v))
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    val id = ModelField.forId[E]()
    val date: ModelField[LocalDateTime, E] = ModelField("date", _.date, v => _.copy(date = v))
    val foreignCurrencyCode: ModelField[String, E] =
      ModelField("foreignCurrencyCode", _.foreignCurrencyCode, v => _.copy(foreignCurrencyCode = v))
    val ratioReferenceToForeignCurrency: ModelField[Double, E] = ModelField(
      "ratioReferenceToForeignCurrency",
      _.ratioReferenceToForeignCurrency,
      v => _.copy(ratioReferenceToForeignCurrency = v))
  }

  // **************** Field-related methods **************** //
  private val allFields: Seq[ModelField.any] = Seq(
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
    ExchangeRateMeasurement.ratioReferenceToForeignCurrency,
  )
  private val fieldToNumberMap: ImmutableBiMap[ModelField.any, Int] =
    CollectionUtils.toBiMapWithStableIntKeys(
      stableNameMapper = field => s"${field.entityType.entityClass.getSimpleName}$$${field.name}$$",
      values = allFields,
    )

  def allFieldsOfEntity(entityType: EntityType.any): Seq[ModelField.any] = {
    allFields.filter(_.entityType == entityType).toVector
  }
  def toNumber(field: ModelField.any): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField.any = fieldToNumberMap.inverse().get(number)
}
