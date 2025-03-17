package hydro.common

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(classOf[TestParameterInjector])
class StringUtilsJvmTest {

  case class StringCase(string: String)

  @Test
  def sanitizeSpecializedCharacters_stripsNewlinesConditionally(
      @TestParameter substituteNonLatin1: Boolean
  ): Unit = {
    assertThat(
      StringUtils.sanitizeSpecializedCharacters("abc\r\ndef\nghi", stripNewlines = true, substituteNonLatin1)
    )
      .isEqualTo("abcdefghi")
    assertThat(
      StringUtils.sanitizeSpecializedCharacters("abc\r\ndef\nghi", stripNewlines = false, substituteNonLatin1)
    )
      .isEqualTo("abc\ndef\nghi")
  }

  @Test
  def sanitizeSpecializedCharacters_replacesTabBySpaces(
      @TestParameter stripNewlines: Boolean,
      @TestParameter substituteNonLatin1: Boolean,
  ): Unit = {
    assertThat(
      StringUtils.sanitizeSpecializedCharacters(
        "\tabc\tdef",
        stripNewlines = stripNewlines,
        substituteNonLatin1,
      )
    )
      .isEqualTo("  abc  def")
  }

  @Test
  def sanitizeSpecializedCharacters_replacesQuotes(
      @TestParameter stripNewlines: Boolean,
      @TestParameter substituteNonLatin1: Boolean,
  ): Unit = {
    assertThat(
      StringUtils.sanitizeSpecializedCharacters(
        "| abc „“def”  ‚‘xyz’ |",
        stripNewlines = stripNewlines,
        substituteNonLatin1,
      )
    )
      .isEqualTo("| abc \"\"def\"  ''xyz' |")
  }

  @Test
  def sanitizeSpecializedCharacters_leavesNormalCharactersAsIs(
      @TestParameter(
        Array(
          "",
          " ' ",
          "?!@     #$%^^",
          "&*()_- +={}",
          "[]\"\\;:<>,./~`",
          "[",
          "]",
          "ü",
          "\u00B7 = ·",
          "\u0020",
          "§",
          "£",
          "©",
          "®",
          "°",
          "±",
          "²",
          "³",
          "µ",
          "·",
          "½",
          "¼",
          "¾",
          "×",
          "¥",
          "€",
          "‰",
          "ÅçèéêëÀÆÇÊõÿ",
        )
      ) s: String,
      @TestParameter stripNewlines: Boolean,
      @TestParameter substituteNonLatin1: Boolean,
  ): Unit = {
    assertThat(StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1)).isEqualTo(s)
    assertThat(StringUtils.sanitizeSpecializedCharacters(f"$s ${s}__$s", stripNewlines, substituteNonLatin1))
      .isEqualTo(f"$s ${s}__$s")
  }

  @Test
  def sanitizeSpecializedCharacters_removesInvisibleSpecialCharacters(
      @TestParameter(
        Array(
          "\r",
          "\u0000",
          "\u0001",
          "\u0002",
          "\u0003",
          "\u0004",
          "\u0005",
          "\u0006",
          "\u0007",
          "\u0008",
          "\u000B",
          "\u000C",
          "\u000D",
          "\u000E",
          "\u000F",
          "\u0010",
          "\u0011",
          "\u0012",
          "\u0013",
          "\u0014",
          "\u0015",
          "\u0016",
          "\u0017",
          "\u0018",
          "\u0019",
          "\u001A",
          "\u001B",
          "\u001C",
          "\u001D",
          "\u001E",
          "\u001F",
          "\u0085",
          "\u007F",
          "\u0081",
          "\u1680",
          "\u180E",
          "\u2000",
          "\u2001",
          "\u2002",
          "\u2003",
          "\u2004",
          "\u2005",
          "\u2006",
          "\u2007",
          "\u2008",
          "\u2009",
          "\u200A",
          "\u200B",
          "\u200C",
          "\u200D",
          "\u2028",
          "\u2029",
          "\u202B",
          "\u202F",
          "\u205F",
          "\u2060",
          "\u3000",
          "\uFEFF",
        )
      )
      s: String,
      @TestParameter stripNewlines: Boolean,
      @TestParameter substituteNonLatin1: Boolean,
  ): Unit = {
    assertThat(StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1)).isEmpty()
    assertThat(StringUtils.sanitizeSpecializedCharacters(f"$s  ${s}__$s", stripNewlines, substituteNonLatin1))
      .isEqualTo("  __")
  }

  @Test
  def sanitizeSpecializedCharacters_replacesSpecialUnicodeCharactersByQuestionMark(
      @TestParameter(
        Array(
          "↡",
          "≡",
          "⍽",
          "⏎",
          "␢",
          "␣",
          "␤",
          "△",
          "⩛",
          "〷",
        )
      )
      s: String,
      @TestParameter stripNewlines: Boolean,
  ): Unit = {
    assertThat(StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1 = true))
      .isEqualTo("?")
    assertThat(
      StringUtils.sanitizeSpecializedCharacters(f"$s++${s}__$s", stripNewlines, substituteNonLatin1 = true)
    )
      .isEqualTo("?++?__?")

    assertThat(StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1 = false))
      .isEqualTo(s)
  }

  @Test
  def toStringWithSpecializedCharactersEscaped_success(
      @TestParameter escapeNewlines: Boolean,
      @TestParameter escapeNonLatin1: Boolean,
  ): Unit = {
    assertThat(
      StringUtils.toStringWithSpecializedCharactersEscaped("ABC\u202Bé€\r", escapeNewlines, escapeNonLatin1)
    )
      .isEqualTo("ABC\\u202Bé€\\u000D")

    assertThat(StringUtils.toStringWithSpecializedCharactersEscaped("\n", escapeNewlines, escapeNonLatin1))
      .isEqualTo(if (escapeNewlines) "\\u000A" else "\n")
    assertThat(StringUtils.toStringWithSpecializedCharactersEscaped("≡", escapeNewlines, escapeNonLatin1))
      .isEqualTo(if (escapeNonLatin1) "\\u2261" else "≡")
  }
}
