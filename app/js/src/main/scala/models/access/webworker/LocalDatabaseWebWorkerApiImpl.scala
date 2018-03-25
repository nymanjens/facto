package models.access.webworker

import jsfacades.{CryptoJs, LokiJs}
import models.access.LocalDatabase.{EncryptingCodex, Impl}
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.scalajs.js

private[webworker] final class LocalDatabaseWebWorkerApiImpl extends LocalDatabaseWebWorkerApi {
  private var lokiDb: LokiJs.Database = _

  override def create(dbName: String, encryptionSecret: String, inMemory: Boolean) = {
    if (inMemory) {
      val lokiDb: LokiJs.Database = LokiJs.Database.inMemoryForTests(
        dbName,
        persistedStringCodex =
          if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
          else new LocalDatabaseWebWorkerApiImpl.EncryptingCodex(encryptionSecret)
      )
    } else {
      lokiDb = LokiJs.Database.persistent(
        dbName,
        persistedStringCodex =
          if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
          else new LocalDatabaseWebWorkerApiImpl.EncryptingCodex(encryptionSecret)
      )
    }

    lokiDb.loadDatabase()
  }

  override def applyWriteOperations(operations: WriteOperation*) = ???

}
object LocalDatabaseWebWorkerApiImpl {
  private final class EncryptingCodex(secret: String) extends LokiJs.PersistedStringCodex {
    private val decodedPrefix = "DECODED"

    override def encodeBeforeSave(dbString: String) = {
      val millis1 = System.currentTimeMillis()
      console.log(s"  Encrypting ${dbString.length / 1e6}Mb String...")
      val result =
        CryptoJs.RC4Drop.encrypt(stringToEncrypt = decodedPrefix + dbString, password = secret).toString()
      val millis2 = System.currentTimeMillis()
      console.log(s"  Encrypting ${dbString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      result
    }

    override def decodeAfterLoad(encodedString: String) = {
      val millis1 = System.currentTimeMillis()
      console.log(s"  Decrypting ${encodedString.length / 1e6}Mb String...")
      val decoded =
        try {
          CryptoJs.RC4Drop
            .decrypt(stringToDecrypt = encodedString, password = secret)
            .toString(CryptoJs.Encoding.Utf8)
        } catch {
          case t: Throwable =>
            console.log(s"  Caught exception while decoding database string: $t")
            ""
        }
      val millis2 = System.currentTimeMillis()
      console.log(
        s"  Decrypting ${encodedString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      if (decoded.startsWith(decodedPrefix)) {
        Some(decoded.substring(decodedPrefix.length))
      } else {
        console.log(s"  Failed to decode database string: ${encodedString.substring(0, 10)}")
        None
      }
    }
  }
}
