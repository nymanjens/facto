package common

import java.io.Serializable

import play.api.mvc.Call
import com.google.common.base.Splitter

import scala.collection.{AbstractSeq, LinearSeqOptimized}
import scala.collection.generic._
import scala.collection.immutable.{LinearSeq, List}
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
    val cleanedUrl = Splitter.on("?").splitToList(url).get(0)

    val newUrl = if (call.url contains "?") {
      s"${call.url}&returnTo=$cleanedUrl"
    } else {
      s"${call.url}?returnTo=$cleanedUrl"
    }
    Call(call.method, newUrl, call.fragment)
  }

  def ++:(call: Call): Call = this appendTo call
}