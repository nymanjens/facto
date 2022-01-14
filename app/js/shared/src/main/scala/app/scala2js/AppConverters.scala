package app.scala2js

import app.models.access.ModelFields
import app.models.accounting._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.scala2js.Scala2Js.MapConverter
import hydro.scala2js.StandardConverters.EntityConverter

import scala.collection.immutable.Seq

object AppConverters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: MapConverter[_ <: Entity] = entityType match {
      case User.Type                    => UserConverter
      case Transaction.Type             => TransactionConverter
      case TransactionGroup.Type        => TransactionGroupConverter
      case BalanceCheck.Type            => BalanceCheckConverter
      case ExchangeRateMeasurement.Type => ExchangeRateMeasurementConverter
    }
    converter.asInstanceOf[MapConverter[E]]
  }

  // **************** Entity converters **************** //
  implicit val UserConverter: EntityConverter[User] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.User.loginName,
      ModelFields.User.passwordHash,
      ModelFields.User.name,
      ModelFields.User.isAdmin,
      ModelFields.User.expandCashFlowTablesByDefault,
      ModelFields.User.expandLiquidationTablesByDefault,
    ),
    toScalaWithoutId = dict =>
      User(
        loginName = dict.getRequired(ModelFields.User.loginName),
        passwordHash = dict.getRequired(ModelFields.User.passwordHash),
        name = dict.getRequired(ModelFields.User.name),
        isAdmin = dict.getRequired(ModelFields.User.isAdmin),
        expandCashFlowTablesByDefault = dict.getRequired(ModelFields.User.expandCashFlowTablesByDefault),
        expandLiquidationTablesByDefault = dict.getRequired(ModelFields.User.expandLiquidationTablesByDefault),
      ),
  )

  implicit val TransactionConverter: EntityConverter[Transaction] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Transaction.transactionGroupId,
      ModelFields.Transaction.issuerId,
      ModelFields.Transaction.beneficiaryAccountCode,
      ModelFields.Transaction.moneyReservoirCode,
      ModelFields.Transaction.categoryCode,
      ModelFields.Transaction.description,
      ModelFields.Transaction.flowInCents,
      ModelFields.Transaction.detailDescription,
      ModelFields.Transaction.tags,
      ModelFields.Transaction.tagsNormalized,
      ModelFields.Transaction.createdDate,
      ModelFields.Transaction.transactionDate,
      ModelFields.Transaction.consumedDate,
    ),
    toScalaWithoutId = dict =>
      Transaction(
        transactionGroupId = dict.getRequired(ModelFields.Transaction.transactionGroupId),
        issuerId = dict.getRequired(ModelFields.Transaction.issuerId),
        beneficiaryAccountCode = dict.getRequired(ModelFields.Transaction.beneficiaryAccountCode),
        moneyReservoirCode = dict.getRequired(ModelFields.Transaction.moneyReservoirCode),
        categoryCode = dict.getRequired(ModelFields.Transaction.categoryCode),
        description = dict.getRequired(ModelFields.Transaction.description),
        flowInCents = dict.getRequired(ModelFields.Transaction.flowInCents),
        detailDescription = dict.getRequired(ModelFields.Transaction.detailDescription),
        tags = dict.getRequired(ModelFields.Transaction.tags),
        createdDate = dict.getRequired(ModelFields.Transaction.createdDate),
        transactionDate = dict.getRequired(ModelFields.Transaction.transactionDate),
        consumedDate = dict.getRequired(ModelFields.Transaction.consumedDate),
      ),
  )

  implicit val TransactionGroupConverter: EntityConverter[TransactionGroup] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.TransactionGroup.createdDate
    ),
    toScalaWithoutId = dict =>
      TransactionGroup(createdDate = dict.getRequired(ModelFields.TransactionGroup.createdDate)),
  )

  implicit val BalanceCheckConverter: EntityConverter[BalanceCheck] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.BalanceCheck.issuerId,
      ModelFields.BalanceCheck.moneyReservoirCode,
      ModelFields.BalanceCheck.balanceInCents,
      ModelFields.BalanceCheck.createdDate,
      ModelFields.BalanceCheck.checkDate,
    ),
    toScalaWithoutId = dict =>
      BalanceCheck(
        issuerId = dict.getRequired(ModelFields.BalanceCheck.issuerId),
        moneyReservoirCode = dict.getRequired(ModelFields.BalanceCheck.moneyReservoirCode),
        balanceInCents = dict.getRequired(ModelFields.BalanceCheck.balanceInCents),
        createdDate = dict.getRequired(ModelFields.BalanceCheck.createdDate),
        checkDate = dict.getRequired(ModelFields.BalanceCheck.checkDate),
      ),
  )

  implicit val ExchangeRateMeasurementConverter: EntityConverter[ExchangeRateMeasurement] =
    new EntityConverter(
      allFieldsWithoutId = Seq(
        ModelFields.ExchangeRateMeasurement.date,
        ModelFields.ExchangeRateMeasurement.foreignCurrencyCode,
        ModelFields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency,
      ),
      toScalaWithoutId = dict =>
        ExchangeRateMeasurement(
          date = dict.getRequired(ModelFields.ExchangeRateMeasurement.date),
          foreignCurrencyCode = dict.getRequired(ModelFields.ExchangeRateMeasurement.foreignCurrencyCode),
          ratioReferenceToForeignCurrency =
            dict.getRequired(ModelFields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency),
        ),
    )
}
