package jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala2js.Converters._

object CryptoJs {

  @JSGlobal("CryptoJS.RC4Drop")
  @js.native
  object RC4Drop extends js.Object {
    def encrypt(stringToEncrypt: String, password: String): EncryptResult = js.native
    def decrypt(stringToDecrypt: String, password: String): DecryptResult = js.native
  }

  @js.native
  trait EncryptResult extends js.Object {
    override def toString(): String = js.native
  }

  @js.native
  trait DecryptResult extends js.Object {
    def toString(encoding: Encoding): String = js.native
  }

  @js.native
  trait Encoding extends js.Object
  object Encoding {
    @JSGlobal("CryptoJS.enc.Utf8")
    @js.native
    object Utf8 extends Encoding
  }
}
