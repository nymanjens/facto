package controllers.helpers.accounting

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import Math.abs
import java.text.NumberFormat
import java.util.Locale

import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import common.accounting.Tags
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import models.accounting.config.Config
import models.accounting.money.Money

object FormUtils {

  def validMoneyReservoir(implicit accountingConfig: Config): Constraint[String] =
    oneOf(accountingConfig.visibleReservoirs.map(_.code))

  def validMoneyReservoirOrNullReservoir(implicit accountingConfig: Config): Constraint[String] =
    oneOf(accountingConfig.moneyReservoirs(includeNullReservoir = true, includeHidden = true).map(_.code))

  def validAccountCode(implicit accountingConfig: Config): Constraint[String] =
    oneOf(accountingConfig.accounts.values.map(_.code))

  def validCategoryCode(implicit accountingConfig: Config): Constraint[String] =
    oneOf(accountingConfig.categories.values.map(_.code))

  def validFlowAsFloat = Constraint[String] { (string: String) =>
    normalizeMoneyString(string) match {
      case flowAsFloatRegex() => Valid
      case _ => invalidWithMessageCode("error.invalid")
    }
  }

  def validTagsString =
    Constraint[String]({ (tagsString: String) =>
      if (tagsString == "") {
        Valid
      } else {
        val tags = Splitter.on(",").split(tagsString).asScala.toList
        val anyInvalid = tags.exists(tag => !Tags.isValidTag(tag))
        if (anyInvalid) {
          invalidWithMessageCode("error.invalid")
        } else {
          Valid
        }
      }
    })

  def flowAsFloatStringToCents(string: String): Long = {
    val normalizedString = normalizeMoneyString(string)
    normalizedString match {
      case flowAsFloatRegex() => (normalizedString.toDouble * 100).round
      case _ => 0
    }
  }

  def invalidWithMessageCode(code: String) = Invalid(Seq(ValidationError(code)))

  private def oneOf(options: Iterable[String]) =
    Constraint[String]({ (input: String) =>
      if (options.toSet.contains(input)) {
        Valid
      } else {
        invalidWithMessageCode("error.invalid")
      }
    })

  /**
    * Trims the given string and only keeps the last punctuation (',' or '.').
    *
    * Examples:
    * "  1,200.39" --> "120.39".
    * "  1.000," --> "1000.".
    */
  private def normalizeMoneyString(s: String): String = {
    val parts = Splitter
      .onPattern("""[\.,]""")
      .trimResults()
      .split(s)
      .asScala
      .toList
    def dotBetweenLastElements(list: List[String]): String = list match {
      case s :: Nil => s
      case first :: last :: Nil => s"$first.$last"
      case first :: rest => first + dotBetweenLastElements(rest)
      case Nil => ""
    }
    dotBetweenLastElements(parts)
  }

  private val flowAsFloatRegex: Regex = """[\-+]{0,1}\d+\.{0,1}\d{0,2}""".r
}
