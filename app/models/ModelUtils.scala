package models

import models.accounting.Money
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object ModelUtils {

  // ********** db helpers ********** //
  val database = Database.forConfig("db.default")

  def dbRun[T](query: DBIO[T]): T = {
    Await.result(database.run(query), Duration.Inf)
  }

  def dbRun[T, C[T]](query: Query[_, T, C]): C[T] = dbRun(query.result)

  // ********** datetime helpers ********** //
  implicit val JodaToSqlDateMapper =
    MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getMillis),
      d => new org.joda.time.DateTime(d.getTime))

  implicit val MoneyToLongMapper =
    MappedColumnType.base[Money, Long](
      money => money.cents,
      cents => Money(cents))
}
