package common

import play.api.mvc.Call

/**
  * Represents a GET parameter in a url. For example: key and value in foo.com/bar?key=value.
  */
trait GetParameter {

  /**
    * Adds this parameter to the given Call.
    */
  def appendTo(call: Call): Call

  /**
    * Adds this parameter to the given URI.
    */
  def appendTo(uri: String): String

  /**
    * Adds this parameter to the given Call.
    *
    * Example: call ++: returnTo
    */
  def ++:(call: Call): Call = this appendTo call

  /**
    * Adds this parameter to the given URI.
    *
    * Example: call ++: returnTo
    */
  def ++:(uri: String): String = this appendTo uri
}

object GetParameter {
  def apply(key: String, value: String): GetParameter = Impl(key, value)

  private[common] case class Impl(key: String, value: String) extends GetParameter {
    override def appendTo(call: Call): Call = {
      val newUrl = this appendTo call.url
      Call(call.method, newUrl, call.fragment)
    }

    override def appendTo(uri: String): String = {
      val escapedValue = value.replace("&", "%26")
      if (uri contains "?") {
        s"$uri&$key=$escapedValue"
      } else {
        s"$uri?$key=$escapedValue"
      }
    }
  }
}
