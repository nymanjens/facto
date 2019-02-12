package hydro.models.slick

import java.nio.ByteBuffer
import java.time.{LocalDateTime => JavaLocalDateTime}
import java.time.Instant
import java.time.ZoneId

import app.api.Picklers._
import boopickle.Default.Pickle
import boopickle.Default.Pickler
import boopickle.Default.Unpickle
import hydro.common.OrderToken
import hydro.common.time.LocalDateTime
import hydro.common.time.LocalDateTimes
import hydro.models.UpdatableEntity.LastUpdateTime
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.higherKinds
import scala.reflect.ClassTag

object SlickUtils {

  // ********** db helpers ********** //
  val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("db.default.slick")
  val dbApi = dbConfig.profile.api

  import dbApi._

  val database = Database.forConfig("db.default")

  def dbRun[T](query: DBIO[T]): T = {
    Await.result(database.run(query), Duration.Inf)
  }

  def dbRun[T, C[T]](query: Query[_, T, C]): C[T] = dbRun(query.result)

  // ********** column mappers ********** //
  def bytesMapperFromPickler[T: Pickler: ClassTag]: ColumnType[T] = {
    def toBytes(value: T) = {
      val byteBuffer = Pickle.intoBytes(value)

      val byteArray = new Array[Byte](byteBuffer.remaining)
      byteBuffer.get(byteArray)
      byteArray
    }
    def toValue(bytes: Array[Byte]) = {
      val byteBuffer = ByteBuffer.wrap(bytes)
      Unpickle[T].fromBytes(byteBuffer)
    }
    MappedColumnType.base[T, Array[Byte]](toBytes, toValue)
  }

  implicit val localDateTimeToSqlDateMapper: ColumnType[LocalDateTime] = {
    val zone = ZoneId.of("Europe/Paris") // This is arbitrary. It just has to be the same in both directions
    def toSql(localDateTime: LocalDateTime) = {
      val javaDate = JavaLocalDateTime.of(localDateTime.toLocalDate, localDateTime.toLocalTime)
      val instant = javaDate.atZone(zone).toInstant
      java.sql.Timestamp.from(instant)
    }
    def toLocalDateTime(sqlTimestamp: java.sql.Timestamp) = {
      val javaDate = sqlTimestamp.toInstant.atZone(zone).toLocalDateTime
      LocalDateTimes.ofJavaLocalDateTime(javaDate)
    }
    MappedColumnType.base[LocalDateTime, java.sql.Timestamp](toSql, toLocalDateTime)
  }

  implicit val instantToSqlTimestampMapper: ColumnType[Instant] =
    MappedColumnType.base[Instant, java.sql.Timestamp](java.sql.Timestamp.from, _.toInstant)

  implicit val finiteDurationToMillisMapper: ColumnType[FiniteDuration] =
    MappedColumnType.base[FiniteDuration, Long](_.toMillis, _.millis)

  implicit val orderTokenToBytesMapper: ColumnType[OrderToken] = {
    val zone = ZoneId.of("Europe/Paris") // This is arbitrary. It just has to be the same in both directions
    def toSql(orderToken: OrderToken) = {
      val bytes = new Array[Byte](orderToken.parts.size * 4)
      val buffer = ByteBuffer.wrap(bytes)
      for (part <- orderToken.parts) {
        buffer.putInt(part)
      }
      bytes
    }
    def toOrderToken(bytes: Array[Byte]) = {
      val byteBuffer = ByteBuffer.wrap(bytes)

      def toOrderTokenParts(): List[Int] = {
        if (byteBuffer.hasRemaining()) {
          byteBuffer.getInt() :: toOrderTokenParts()
        } else {
          Nil
        }
      }

      OrderToken(toOrderTokenParts())
    }
    MappedColumnType.base[OrderToken, Array[Byte]](toSql, toOrderToken)
  }

  implicit val lastUpdateTimeToBytesMapper: ColumnType[LastUpdateTime] = bytesMapperFromPickler
}
