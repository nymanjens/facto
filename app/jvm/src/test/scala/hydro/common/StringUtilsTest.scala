package hydro.common

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(classOf[TestParameterInjector])
class StringUtilsTest {

  case class StringCase(string: String)

  @Test
  def cleanupForSpecializedCharacters_stripsNewlinesConditionally(): Unit = {
    assertThat(StringUtils.cleanupForSpecializedCharacters("abc\r\ndef\nghi", stripNewlines = true))
      .isEqualTo("abcdefghi")
    assertThat(StringUtils.cleanupForSpecializedCharacters("abc\r\ndef\nghi", stripNewlines = false))
      .isEqualTo("abc\ndef\nghi")
  }

  @Test
  def cleanupForSpecializedCharacters_replacesTabBySpaces(@TestParameter stripNewlines: Boolean): Unit = {
    assertThat(StringUtils.cleanupForSpecializedCharacters("\tabc\tdef", stripNewlines = stripNewlines))
      .isEqualTo("  abc  def")
  }

  @Test
  def cleanupForSpecializedCharacters_leavesNormalCharactersAsIs(
      @TestParameter(
        Array(
          "",
          " ' ",
          "?!@     #$%^^",
          "&*()_- +={}",
          "[]\"\\;:<>,./~`",
          "ü",
          "\u00B7 = ·",
        )
      ) s: String,
      @TestParameter stripNewlines: Boolean,
  ): Unit = {
    assertThat(StringUtils.cleanupForSpecializedCharacters(s, stripNewlines)).isEqualTo(s)
  }

  @Test
  def cleanupForSpecializedCharacters_removesInvisibleSpecialCharacters(
      @TestParameter(
        Array(
          "\r",
          "\u000B",
          "\u000C",
          "\u0085",
          "\u00A0",
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
  ): Unit = {
    assertThat(StringUtils.cleanupForSpecializedCharacters(s, stripNewlines)).isEmpty()
    assertThat(StringUtils.cleanupForSpecializedCharacters(f"$s  ${s}__$s", stripNewlines)).isEqualTo("  __")
  }

  @Test
  def cleanupForSpecializedCharacters_replacesSpecialCharactersByQuestionMark(
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
    assertThat(StringUtils.cleanupForSpecializedCharacters(s, stripNewlines)).isEqualTo("?")
    assertThat(StringUtils.cleanupForSpecializedCharacters(f"$s++${s}__$s", stripNewlines))
      .isEqualTo("?++?__?")
  }
}
