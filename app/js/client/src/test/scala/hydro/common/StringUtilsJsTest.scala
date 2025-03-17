package hydro.common

import hydro.common.testing.Awaiter
import utest._
import utest.TestSuite

import java.util.concurrent.atomic.AtomicLong
import scala.async.Async.async
import scala.async.Async.await
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise

object StringUtilsJsTest extends TestSuite {

  override def tests = TestSuite {

    "sanitizeSpecializedCharacters_stripsNewlinesConditionally" - {
      testMultipleCases(
        for {
          substituteNonLatin1 <- Seq(true, false)
        } yield TestCase(
          name = f"substituteNonLatin1=$substituteNonLatin1",
          () => {
            StringUtils.sanitizeSpecializedCharacters(
              "abc\r\ndef\nghi",
              stripNewlines = true,
              substituteNonLatin1,
            ) ==> "abcdefghi"

            StringUtils.sanitizeSpecializedCharacters(
              "abc\r\ndef\nghi",
              stripNewlines = false,
              substituteNonLatin1,
            ) ==> "abc\ndef\nghi"
          },
        )
      )
    }

    "sanitizeSpecializedCharacters_replacesTabBySpaces" - {
      testMultipleCases(
        for {
          stripNewlines <- Seq(true, false)
          substituteNonLatin1 <- Seq(true, false)
        } yield TestCase(
          name = f"stripNewlines=$stripNewlines substituteNonLatin1=$substituteNonLatin1",
          () => {
            StringUtils.sanitizeSpecializedCharacters(
              "\tabc\tdef",
              stripNewlines = stripNewlines,
              substituteNonLatin1,
            ) ==> "  abc  def"
          },
        )
      )
    }

    "sanitizeSpecializedCharacters_replacesQuotes" - {
      testMultipleCases(
        for {
          stripNewlines <- Seq(true, false)
          substituteNonLatin1 <- Seq(true, false)
        } yield TestCase(
          name = f"stripNewlines=$stripNewlines substituteNonLatin1=$substituteNonLatin1",
          () => {
            StringUtils.sanitizeSpecializedCharacters(
              "| abc „“def”  ‚‘xyz’ |",
              stripNewlines = stripNewlines,
              substituteNonLatin1,
            ) ==> "| abc \"\"def\"  ''xyz' |"
          },
        )
      )
    }

    "sanitizeSpecializedCharacters_leavesNormalCharactersAsIs" - {
      testMultipleCases(
        for {
          stripNewlines <- Seq(true, false)
          substituteNonLatin1 <- Seq(true, false)
          s <- Seq(
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
        } yield TestCase(
          name = f"stripNewlines=$stripNewlines substituteNonLatin1=$substituteNonLatin1 s=${StringUtils
            .toStringWithSpecializedCharactersEscaped(s, escapeNewlines = true, escapeNonLatin1 = true)}",
          () => {
            StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1) ==> s
            StringUtils.sanitizeSpecializedCharacters(
              f"$s ${s}__$s",
              stripNewlines,
              substituteNonLatin1,
            ) ==> f"$s ${s}__$s"
          },
        )
      )
    }

    "sanitizeSpecializedCharacters_removesInvisibleSpecialCharacters" - {
      testMultipleCases(
        for {
          stripNewlines <- Seq(true, false)
          substituteNonLatin1 <- Seq(true, false)
          s <- Seq(
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
        } yield TestCase(
          name = f"stripNewlines=$stripNewlines substituteNonLatin1=$substituteNonLatin1 s=${StringUtils
            .toStringWithSpecializedCharactersEscaped(s, escapeNewlines = true, escapeNonLatin1 = true)}",
          () => {
            StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1) ==> ""
            StringUtils.sanitizeSpecializedCharacters(
              f"$s  ${s}__$s",
              stripNewlines,
              substituteNonLatin1,
            ) ==> "  __"
          },
        )
      )
    }

    "sanitizeSpecializedCharacters_replacesSpecialUnicodeCharactersByQuestionMark" - {
      testMultipleCases(
        for {
          stripNewlines <- Seq(true, false)
          s <- Seq(
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
        } yield TestCase(
          name = f"stripNewlines=$stripNewlines s=${StringUtils
            .toStringWithSpecializedCharactersEscaped(s, escapeNewlines = true, escapeNonLatin1 = true)}",
          () => {
            StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1 = true) ==> "?"
            StringUtils.sanitizeSpecializedCharacters(
              f"$s++${s}__$s",
              stripNewlines,
              substituteNonLatin1 = true,
            ) ==> "?++?__?"
            StringUtils.sanitizeSpecializedCharacters(s, stripNewlines, substituteNonLatin1 = false) ==> s
          },
        )
      )
    }

    "toStringWithSpecializedCharactersEscaped_success" - {
      testMultipleCases(
        for {
          escapeNewlines <- Seq(true, false)
          escapeNonLatin1 <- Seq(true, false)
        } yield TestCase(
          name = f"escapeNewlines=$escapeNewlines escapeNonLatin1=$escapeNonLatin1",
          () => {
            StringUtils.toStringWithSpecializedCharactersEscaped(
              "ABC\u202Bé€\r",
              escapeNewlines,
              escapeNonLatin1,
            ) ==> "ABC\\u202Bé€\\u000D"

            StringUtils.toStringWithSpecializedCharactersEscaped(
              "\n",
              escapeNewlines,
              escapeNonLatin1,
            ) ==> (if (escapeNewlines) "\\u000A" else "\n")
            StringUtils
              .toStringWithSpecializedCharactersEscaped("≡", escapeNewlines, escapeNonLatin1) ==> (if (
                                                                                                     escapeNonLatin1
                                                                                                   ) "\\u2261"
                                                                                                   else "≡")
          },
        )
      )
    }
  }

// *********************** Test infrastructure *********************** //
  case class TestCase(name: String, assertions: () => Unit)

  private def testMultipleCases(cases: Iterable[TestCase]): Unit = {
    val errors = mutable.Buffer[String]()

    for ((c, i) <- cases.zipWithIndex) {
      try {
        c.assertions()
      } catch {
        case e: Throwable => errors.append(f"[${i}] For '${c.name}':\n${e.getMessage}")
      }
    }

    if (errors.nonEmpty) {
      throw new java.lang.AssertionError(
        "Multiple cases runner: Caught errors:\n\n" + errors.mkString("\n\n")
      )
    }
  }
}
