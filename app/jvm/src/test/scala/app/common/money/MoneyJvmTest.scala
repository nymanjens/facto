package app.common.money

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.runner.RunWith
import org.junit.Test

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.collection.immutable.Seq

@RunWith(classOf[TestParameterInjector])
class MoneyJvmTest {

  @TestParameters(
    Array(
      "{input: '1.23', expected: 123}",
      "{input: '1.2', expected: 120}",
      "{input: '1,23', expected: 123}",
      "{input: '1,2', expected: 120}",
      "{input: '12345.', expected: 1234500}",
      "{input: '.12', expected: 12}",
      "{input: ',12', expected: 12}",
      "{input: '12,', expected: 1200}",
      "{input: '12.', expected: 1200}",
      "{input: '1,234,567.8', expected: 123456780}",
      "{input: '1,234,567', expected: 123456700}",
      "{input: '1.234.567', expected: 123456700}",
      "{input: '1.234.567,8', expected: 123456780}",
      "{input: '1,234', expected: 123400}",
      "{input: '1.234', expected: 123400}",
      "{input: '1 234', expected: 123400}",
      "{input: ' 1 , 234 . 56 ', expected: 123456}",
      "{input: '12.\t', expected: 1200}",
      "{input: '€12', expected: 1200}",
      "{input: '€12.', expected: 1200}",
      "{input: '€.12', expected: 12}",
      "{input: '$.12', expected: 12}",
      "{input: '1.23k', expected: 123000}",
      "{input: '1.23M', expected: 123000000}",
      "{input: '20k', expected: 2000000}",
    )
  )
  @Test
  def floatStringToCents_success(input: String, expected: Long): Unit = {
    assertThat(
      Money.tryFloatStringToCents(input)
    ) isEqualTo Success(expected)

    assertWithMessage("Inverted case").that(Money.tryFloatStringToCents("-" + input)) isEqualTo Success(
      -expected
    )
    assertWithMessage("Case with plus").that(Money.tryFloatStringToCents("+" + input)) isEqualTo Success(
      expected
    )
  }

  @Test
  def floatStringToCents_failure(
      @TestParameter(
        Array(
          "1.2.3",
          "",
          "--1",
          ".",
          "1.0000",
          "1,0000",
          "1.00.00",
          "1,00,00",
          "1.00.000",
          "1,00,000",
          "1.0000.000",
          "1,0000,000",
          ",",
          ".123",
          ",123",
          "abc",
        )
      ) input: String
  ): Unit = {
    assertThat(
      Money.floatStringToCents(input)
    ) isEqualTo None
  }
}
