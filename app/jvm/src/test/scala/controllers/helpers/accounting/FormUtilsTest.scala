package controllers.helpers.accounting

import scala.collection.immutable.Seq
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.accounting.config.{Account, MoneyReservoir}
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class FormUtilsTest extends Specification {

  "validFlowAsFloat" in new WithApplication {
    val constraint = FormUtils.validFlowAsFloat

    constraint("abc") mustNotEqual Valid
    constraint("0x123") mustNotEqual Valid
    constraint("123.123") mustNotEqual Valid

    constraint("123.44") mustEqual Valid
    constraint("  1,991,123 . 44 ") mustEqual Valid
    constraint("1.000,") mustEqual Valid
  }

  "flowAsFloatStringToCents" in new WithApplication {
    FormUtils.flowAsFloatStringToCents("123.44") mustEqual 12344L
    FormUtils.flowAsFloatStringToCents("  1,991,123 . 44 ") mustEqual 199112344L
    FormUtils.flowAsFloatStringToCents("  1.000,") mustEqual 100000L
  }

  "validTagsString"  in new WithApplication {
    val constraint = FormUtils.validTagsString
    constraint("") mustEqual Valid
    constraint("abc") mustEqual Valid
    constraint("abc,def") mustEqual Valid
    constraint("abc def") mustNotEqual Valid
    constraint(" ") mustNotEqual Valid
    constraint(" abc") mustNotEqual Valid
  }
}
