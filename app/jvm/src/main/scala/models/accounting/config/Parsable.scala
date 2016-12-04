package models.accounting.config

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import java.util.Collections

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.ImmutableList
import common.Require.requireNonNull
import models.accounting.config.{Account => ParsedAccount, Category => ParsedCategory, Config => ParsedConfig,
Constants => ParsedConstants, MoneyReservoir => ParsedMoneyReservoir, Template => ParsedTemplate}
import models.accounting.config.Account.{SummaryTotalRowDef => ParsedSummaryTotalRowDef}
import models.accounting.config.MoneyReservoir.NullMoneyReservoir
import models.accounting.money.Money

object Parsable {

  case class Config(accounts: java.util.List[Account],
                    categories: java.util.List[Category],
                    moneyReservoirs: java.util.List[MoneyReservoir],
                    templates: java.util.List[Template],
                    constants: Constants) {
    def this() = this(null, null, null, null, null)

    def parse: ParsedConfig = {
      requireNonNull(accounts, categories, moneyReservoirs, templates, constants)
      val parsedAccounts = toListMap(accounts)(_.code, _.parse)
      val parsedCategories = toListMap(categories)(_.code, _.parse)
      val parsedReservoirs = toListMap(moneyReservoirs)(_.code, _.parse)
      val parsedTemplates = templates.asScala.toVector map { tpl =>
        tpl.parse(parsedAccounts, parsedReservoirs, parsedCategories)
      }

      // Validation
      parsedAccounts.values foreach (_.validateCodes(parsedReservoirs.values))

      ParsedConfig(parsedAccounts, parsedCategories, parsedReservoirs, parsedTemplates, constants.parse)
    }
  }

  case class Account(code: String,
                     longName: String,
                     shorterName: String,
                     veryShortName: String,
                     userLoginName: /* nullable */ String,
                     defaultCashReservoirCode: /* nullable */ String,
                     defaultElectronicReservoirCode: String,
                     categories: java.util.List[Category],
                     summaryTotalRows: /* nullable */ java.util.List[Account.SummaryTotalRowDef]) {
    def this() = this(null, null, null, null, null, null, null, null, null)

    def parse: ParsedAccount = {
      var nonNullSummaryTotalRows = if (summaryTotalRows == null) ImmutableList.of(Account.SummaryTotalRowDef.default) else summaryTotalRows
      ParsedAccount(
        code = code,
        longName = longName,
        shorterName = shorterName,
        veryShortName = veryShortName,
        userLoginName = Option(userLoginName),
        defaultCashReservoirCode = Option(defaultCashReservoirCode),
        defaultElectronicReservoirCode = defaultElectronicReservoirCode,
        categories = checkNotNull(categories).asScala.toList.map(_.parse),
        summaryTotalRows = nonNullSummaryTotalRows.asScala.toList.map(_.parse))
    }
  }
  object Account {
    case class SummaryTotalRowDef(rowTitleHtml: String, categoriesToIgnore: java.util.List[Category]) {
      def this() = this(null, null)

      def parse: ParsedSummaryTotalRowDef = ParsedSummaryTotalRowDef(
        rowTitleHtml = checkNotNull(rowTitleHtml),
        categoriesToIgnore = checkNotNull(categoriesToIgnore).asScala.map(_.parse).toSet)
    }
    object SummaryTotalRowDef {
      val default: SummaryTotalRowDef = SummaryTotalRowDef("<b>Total</b>", Collections.emptyList[Category])
    }
  }

  case class Category(code: String, name: String, helpText: String) {
    def this() = this(null, null, helpText = "")

    def parse: ParsedCategory = {
      ParsedCategory(code, name, helpText)
    }
  }


  case class MoneyReservoir(code: String,
                            name: String,
                            shorterName: /* nullable */ String,
                            owner: Account,
                            hidden: Boolean,
                            currency: /* nullable */ String) {
    def this() = this(null, null, null, null, hidden = false, null)

    def parse: ParsedMoneyReservoir = {
      val parsedShorterName = if (shorterName == null) name else shorterName
      ParsedMoneyReservoir(code, name, parsedShorterName, owner.parse, hidden, currencyCode = Option(currency))
    }
  }

  case class Template(code: String,
                      name: String,
                      placement: java.util.List[String],
                      onlyShowForUserLoginNames: /* nullable */ java.util.List[String],
                      zeroSum: Boolean,
                      icon: String,
                      transactions: java.util.List[Template.Transaction]) {

    def this() = this(null, null, null, null, zeroSum = false, icon = "fa-plus-square", null)

    def parse(accounts: Map[String, ParsedAccount],
              reservoirs: Map[String, ParsedMoneyReservoir],
              categories: Map[String, ParsedCategory]): ParsedTemplate = {
      ParsedTemplate(
        code = code,
        name = name,
        placement = checkNotNull(placement).asScala.toSet map ParsedTemplate.Placement.fromString,
        onlyShowForUserLoginNames = Option(onlyShowForUserLoginNames) map (_.asScala.toSet),
        zeroSum = zeroSum,
        iconClass = icon,
        transactions = checkNotNull(transactions).asScala.toList map (_.parse(accounts, reservoirs, categories)))
    }
  }

  object Template {
    case class Transaction(beneficiaryCode: /* nullable */ String,
                           moneyReservoirCode: /* nullable */ String,
                           categoryCode: /* nullable */ String,
                           description: String,
                           flowAsFloat: Double,
                           tagsString: String) {
      def this() = this(null, null, null, description = "", flowAsFloat = 0, tagsString = "")

      def parse(accounts: Map[String, ParsedAccount],
                reservoirs: Map[String, ParsedMoneyReservoir],
                categories: Map[String, ParsedCategory]): ParsedTemplate.Transaction = {
        def validateCode(values: Set[String])(code: String): String = {
          if (!(code contains "$")) {
            require(values contains code, s"Illegal code '$code' (possibilities = $values)")
          }
          code
        }
        val reservoirsIncludingNull = reservoirs ++ Map(NullMoneyReservoir.code -> NullMoneyReservoir)
        ParsedTemplate.Transaction(
          beneficiaryCodeTpl = Option(beneficiaryCode) map validateCode(accounts.keySet),
          moneyReservoirCodeTpl = Option(moneyReservoirCode) map validateCode(reservoirsIncludingNull.keySet),
          categoryCodeTpl = Option(categoryCode) map validateCode(categories.keySet),
          descriptionTpl = description,
          flowInCents = (flowAsFloat.toDouble * 100).round,
          tagsString = tagsString)
      }
    }
  }


  case class Constants(commonAccount: Account,
                       accountingCategory: Category,
                       endowmentCategory: Category,
                       liquidationDescription: String) {
    def this() = this(null, null, null, liquidationDescription = "Liquidation")

    def parse: ParsedConstants = {
      ParsedConstants(
        commonAccount = commonAccount.parse,
        accountingCategory = accountingCategory.parse,
        endowmentCategory = endowmentCategory.parse,
        liquidationDescription = liquidationDescription)
    }
  }


  private def toListMap[T, K, V](list: java.util.List[T])(keyGetter: T => K, valueGetter: T => V): ListMap[K, V] = {
    checkNotNull(list)
    val tuples = list.asScala.map(t => (keyGetter(t), valueGetter(t)))
    val resultBuilder = ListMap.newBuilder[K, V]
    tuples.foreach(resultBuilder += _)
    resultBuilder.result
  }
}
