package app.scala2js

import app.models._
import app.models.access.ModelField
import app.models.accounting._
import app.models.modification._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.scala2js.Scala2Js.Converter
import hydro.scala2js.Scala2Js.MapConverter
import hydro.scala2js.StandardConverters
import hydro.scala2js.StandardConverters.EntityConverter

import scala.collection.immutable.Seq

object AppConverters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: MapConverter[_ <: Entity] = entityType match {
      case EntityType.UserType                    => UserConverter
      case EntityType.TransactionType             => TransactionConverter
      case EntityType.TransactionGroupType        => TransactionGroupConverter
      case EntityType.BalanceCheckType            => BalanceCheckConverter
      case EntityType.ExchangeRateMeasurementType => ExchangeRateMeasurementConverter
    }
    converter.asInstanceOf[MapConverter[E]]
  }

  // **************** General converters **************** //
  implicit val EntityTypeConverter: Converter[EntityType.any] = StandardConverters.enumConverter(
    EntityType.UserType,
    EntityType.TransactionType,
    EntityType.TransactionGroupType,
    EntityType.BalanceCheckType,
    EntityType.ExchangeRateMeasurementType)

  // **************** Entity converters **************** //
  implicit val UserConverter: EntityConverter[User] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.User.loginName,
      ModelField.User.passwordHash,
      ModelField.User.name,
      ModelField.User.isAdmin,
      ModelField.User.expandCashFlowTablesByDefault,
      ModelField.User.expandLiquidationTablesByDefault,
    ),
    toScalaWithoutId = dict =>
      User(
        loginName = dict.getRequired(ModelField.User.loginName),
        passwordHash = dict.getRequired(ModelField.User.passwordHash),
        name = dict.getRequired(ModelField.User.name),
        isAdmin = dict.getRequired(ModelField.User.isAdmin),
        expandCashFlowTablesByDefault = dict.getRequired(ModelField.User.expandCashFlowTablesByDefault),
        expandLiquidationTablesByDefault = dict.getRequired(ModelField.User.expandLiquidationTablesByDefault)
    )
  )

  implicit val TransactionConverter: EntityConverter[Transaction] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.Transaction.transactionGroupId,
      ModelField.Transaction.issuerId,
      ModelField.Transaction.beneficiaryAccountCode,
      ModelField.Transaction.moneyReservoirCode,
      ModelField.Transaction.categoryCode,
      ModelField.Transaction.description,
      ModelField.Transaction.flowInCents,
      ModelField.Transaction.detailDescription,
      ModelField.Transaction.tags,
      ModelField.Transaction.createdDate,
      ModelField.Transaction.transactionDate,
      ModelField.Transaction.consumedDate,
    ),
    toScalaWithoutId = dict =>
      Transaction(
        transactionGroupId = dict.getRequired(ModelField.Transaction.transactionGroupId),
        issuerId = dict.getRequired(ModelField.Transaction.issuerId),
        beneficiaryAccountCode = dict.getRequired(ModelField.Transaction.beneficiaryAccountCode),
        moneyReservoirCode = dict.getRequired(ModelField.Transaction.moneyReservoirCode),
        categoryCode = dict.getRequired(ModelField.Transaction.categoryCode),
        description = dict.getRequired(ModelField.Transaction.description),
        flowInCents = dict.getRequired(ModelField.Transaction.flowInCents),
        detailDescription = dict.getRequired(ModelField.Transaction.detailDescription),
        tags = dict.getRequired(ModelField.Transaction.tags),
        createdDate = dict.getRequired(ModelField.Transaction.createdDate),
        transactionDate = dict.getRequired(ModelField.Transaction.transactionDate),
        consumedDate = dict.getRequired(ModelField.Transaction.consumedDate)
    )
  )

  implicit val TransactionGroupConverter: EntityConverter[TransactionGroup] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.TransactionGroup.createdDate,
    ),
    toScalaWithoutId =
      dict => TransactionGroup(createdDate = dict.getRequired(ModelField.TransactionGroup.createdDate))
  )

  implicit val BalanceCheckConverter: EntityConverter[BalanceCheck] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.BalanceCheck.issuerId,
      ModelField.BalanceCheck.moneyReservoirCode,
      ModelField.BalanceCheck.balanceInCents,
      ModelField.BalanceCheck.createdDate,
      ModelField.BalanceCheck.checkDate,
    ),
    toScalaWithoutId = dict =>
      BalanceCheck(
        issuerId = dict.getRequired(ModelField.BalanceCheck.issuerId),
        moneyReservoirCode = dict.getRequired(ModelField.BalanceCheck.moneyReservoirCode),
        balanceInCents = dict.getRequired(ModelField.BalanceCheck.balanceInCents),
        createdDate = dict.getRequired(ModelField.BalanceCheck.createdDate),
        checkDate = dict.getRequired(ModelField.BalanceCheck.checkDate)
    )
  )

  implicit val ExchangeRateMeasurementConverter: EntityConverter[ExchangeRateMeasurement] =
    new EntityConverter(
      allFieldsWithoutId = Seq(
        ModelField.ExchangeRateMeasurement.date,
        ModelField.ExchangeRateMeasurement.foreignCurrencyCode,
        ModelField.ExchangeRateMeasurement.ratioReferenceToForeignCurrency,
      ),
      toScalaWithoutId = dict =>
        ExchangeRateMeasurement(
          date = dict.getRequired(ModelField.ExchangeRateMeasurement.date),
          foreignCurrencyCode = dict.getRequired(ModelField.ExchangeRateMeasurement.foreignCurrencyCode),
          ratioReferenceToForeignCurrency =
            dict.getRequired(ModelField.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
      )
    )
}
