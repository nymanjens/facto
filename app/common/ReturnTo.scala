package common

import play.api.mvc.Call

/**
  * Represents the URL to return to after the current flow is finished.
  */
case class ReturnTo(url: String) {

  /**
    * Adds this url to the given Call.
    *
    * Don't pass this.url as route parameter because Play Framework will escape
    * the slashes, which is annoying when setting up Facto with Apache.
    */
  def appendTo(call: Call): Call = {
    val newUrl = if (call.url contains "?") {
      s"${call.url}&returnTo=${this.url}"
    } else {
      s"${call.url}?returnTo=${this.url}"
    }
    Call(call.method, newUrl, call.fragment)
  }

  /**
    * Adds this url to the given Call.
    *
    * Example: call ++: returnTo
    *
    * Don't pass this.url as route parameter because Play Framework will escape
    * the slashes, which is annoying when setting up Facto with Apache.
    */
  def ++:(call: Call): Call = this appendTo call
}
