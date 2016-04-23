package models.accounting.config

import java.util.Collections

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

import com.google.common.collect.ImmutableList
import play.twirl.api.Html

import models.accounting.config.{Config => ParsedConfig, Account => ParsedAccount, Category => ParsedCategory, MoneyReservoir => ParsedMoneyReservoir, Constants => ParsedConstants}
import models.accounting.config.Account.{SummaryTotalRowDef => ParsedSummaryTotalRowDef}

object Parsable {

  private def toListMap[T, K, V](list: java.util.List[T])(keyGetter: T => K, valueGetter: T => V): ListMap[K, V] = {
    val tuples = list.asScala.map(t => (keyGetter(t), valueGetter(t)))
    val resultBuilder = ListMap.newBuilder[K, V]
    tuples.foreach(resultBuilder += _)
    resultBuilder.result
  }

  case class Config(accounts: java.util.List[Account],
                    categories: java.util.List[Category],
                    moneyReservoirs: java.util.List[MoneyReservoir],
                    constants: Constants) {
    def this() = this(null, null, null, null)

    def parse: ParsedConfig = {
      val unknownAccount = ParsedAccount("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN")
      ParsedConfig(
        accounts = toListMap(accounts)(_.code, _.parse).withDefault(code => ParsedAccount(code, code, code, code)),
        categories = toListMap(categories)(_.code, _.parse).withDefault(code => ParsedCategory(code, code)),
        moneyReservoirs = toListMap(moneyReservoirs)(_.code, _.parse).withDefault(code => ParsedMoneyReservoir(code, code, unknownAccount)),
        constants = constants.parse)
    }
  }

  case class Account(code: String,
                     longName: String,
                     shorterName: String,
                     veryShortName: String,
                     userLoginName: /* nullable */ String,
                     categories: java.util.List[Category],
                     summaryTotalRows: /* nullable */ java.util.List[SummaryTotalRowDef]) {
    def this() = this(null, null, null, null, null, null, null)

    def parse = {
      var nonNullSummaryTotalRows = if (summaryTotalRows == null) ImmutableList.of(SummaryTotalRowDef.default) else summaryTotalRows
      ParsedAccount(
        code = code,
        longName = longName,
        shorterName = shorterName,
        veryShortName = veryShortName,
        userLoginName = if (userLoginName == null) None else Some(userLoginName),
        categories = categories.asScala.toList.map(_.parse),
        summaryTotalRows = nonNullSummaryTotalRows.asScala.toList.map(_.parse))
    }
  }

  case class SummaryTotalRowDef(rowTitleHtml: String, categoriesToIgnore: java.util.List[Category]) {
    def this() = this(null, null)

    def parse = ParsedSummaryTotalRowDef(
      rowTitleHtml = Html(rowTitleHtml),
      categoriesToIgnore = categoriesToIgnore.asScala.map(_.parse).toSet)
  }
  object SummaryTotalRowDef {
    val default: SummaryTotalRowDef = SummaryTotalRowDef("<b>Total</b>", Collections.emptyList[Category])
  }

  case class Category(code: String, name: String, helpText: String) {
    def this() = this(null, null, helpText = "")

    def parse: ParsedCategory = {
      ParsedCategory(code, name, helpText)
    }
  }


  case class MoneyReservoir(code: String, name: String, owner: Account) {
    def this() = this(null, null, null)

    def parse: ParsedMoneyReservoir = {
      ParsedMoneyReservoir(code, name, owner.parse)
    }
  }

  case class Constants(commonAccount: Account,
                       accountingCategory: Category,
                       endowmentCategory: Category,
                       defaultElectronicMoneyReservoirs: java.util.List[MoneyReservoir],
                       defaultCurrencySymbol: String,
                       liquidationDescription: String) {
    def this() = this(null, null, null, null, null, liquidationDescription = "Liquidation")

    def parse: ParsedConstants = {
      ParsedConstants(
        commonAccount = commonAccount.parse,
        accountingCategory = accountingCategory.parse,
        endowmentCategory = endowmentCategory.parse,
        defaultElectronicMoneyReservoirByAccount = toListMap(defaultElectronicMoneyReservoirs)(_.parse.owner, _.parse),
        liquidationDescription = liquidationDescription,
        defaultCurrencySymbol = defaultCurrencySymbol)
    }
  }

}
