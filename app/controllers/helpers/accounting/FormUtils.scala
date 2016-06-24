package controllers.helpers.accounting

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import Math.abs

import com.google.common.base.Splitter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import models.accounting.{Money, Tag}
import models.accounting.config.Config


object FormUtils {

  def validMoneyReservoir: Constraint[String] = oneOf(Config.visibleReservoirs.map(_.code))

  def validMoneyReservoirOrNullReservoir: Constraint[String] = oneOf(Config.visibleReservoirs(includeNullReservoir = true).map(_.code))

  def validAccountCode: Constraint[String] = oneOf(Config.accounts.values.map(_.code))

  def validCategoryCode: Constraint[String] = oneOf(Config.categories.values.map(_.code))

  def validFlowAsFloat = Constraint[String]("error.invalid")({
    case flowAsFloatRegex() => Valid
    case _ => invalidWithMessageCode("error.invalid")
  })

  def validTagsString = Constraint[String]("error.invalid")({
    tagsString =>
      if (tagsString == "") {
        Valid
      } else {
        val tags = Splitter.on(",").split(tagsString).asScala.toList
        val anyInvalid = tags.exists(tag => !Tag.isValidTagName(tag))
        if (anyInvalid) {
          invalidWithMessageCode("error.invalid")
        } else {
          Valid
        }
      }
  })

  def flowAsFloatStringToMoney(flowAsFloat: String): Money = flowAsFloat match {
    case flowAsFloatRegex() => Money((flowAsFloat.replace(',', '.').toDouble * 100).round)
    case _ => Money(0)
  }

  def invalidWithMessageCode(code: String) = Invalid(Seq(ValidationError(code)))

  private def oneOf(options: Iterable[String]) = Constraint[String]("error.invalid")({
    input =>
      if (options.toSet.contains(input)) {
        Valid
      } else {
        invalidWithMessageCode("error.invalid")
      }
  })

  private val flowAsFloatRegex: Regex = """[\-+]{0,1}\d+[\.,]{0,1}\d{0,2}""".r
}
