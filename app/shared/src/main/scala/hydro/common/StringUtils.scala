package hydro.common

import java.nio.charset.Charset

object StringUtils {

  private val EDGE_CASE_REPLACEMENTS: GuavaReplacement.ImmutableBiMap[Char, String] = GuavaReplacement.ImmutableBiMap
    .builder()
    // Workaround for odd bug in p{Z} regex below that also seems to be filtering out newlines
    .put('\n', "%)A_[7|%*&")
    // Supported by latin1, but with a different code
    .put('€', "%)B_[7|%*&")
    // Supported by latin1, but with a different code
    .put('‰', "%)C_[7|%*&")
    .build()

  def cleanupSpecializedCharacters(
      input: String,
      stripNewlines: Boolean,
      substituteNonLatin1: Boolean,
  ): String = {
    var result = input

    if (stripNewlines) {
      result = result.replace("\n", "")
    }

    // Workarounds for operations below that do too much
    for(char <- EDGE_CASE_REPLACEMENTS.keySet){
      result = result.replace(char.toString, EDGE_CASE_REPLACEMENTS.get(char))
    }

    // Special case: Tab -> "  "
    result = result.replace("\t", "  ")

    // Special case: Speical quotes to normal quotes
    result = result.replace("‘", "'")
    result = result.replace("‚", "'")
    result = result.replace("’", "'")
    result = result.replace("“", "\"")
    result = result.replace("„", "\"")
    result = result.replace("”", "\"")

    // Strip any kind of whitespace or invisible separator.
    result = result.replaceAll("[\\p{Z}&&[^ \n]]", "")

    // Strip invisible control characters and unused code points
    result = result.replaceAll("\\p{C}", "")

    if (substituteNonLatin1) {
      // Strip all unicode characters that are not supported by Latin1
      val charset = Charset.forName("ISO-8859-1")
      result = new String(result.getBytes(charset), charset)
    }

    // Reverse EDGE_CASE_REPLACEMENTS
    for (s <- EDGE_CASE_REPLACEMENTS.inverse().keySet) {
      result = result.replace(s, EDGE_CASE_REPLACEMENTS.inverse().get(s).toString)
    }

    result
  }
}
