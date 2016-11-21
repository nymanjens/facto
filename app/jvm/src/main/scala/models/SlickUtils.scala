package models

import models.accounting.money.Money

import scala.concurrent.Await
import scala.concurrent.duration._
import slick.backend.DatabaseConfig
import slick.driver.JdbcDriver

object SlickUtils {

  // ********** db helpers ********** //
  val dbConfig: DatabaseConfig[JdbcDriver] = DatabaseConfig.forConfig("db.default.slick")
  val dbApi = dbConfig.driver.api
  import dbApi._
  val database = Database.forConfig("db.default")

  def dbRun[T](query: DBIO[T]): T = {
    Await.result(database.run(query), Duration.Inf)
  }

  def dbRun[T, C[T]](query: Query[_, T, C]): C[T] = dbRun(query.result)

  // ********** datetime helpers ********** //
  implicit val JodaToSqlDateMapper =
    MappedColumnType.base[java.time.Instant, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getMillis),
      d => new java.time.Instant(d.getTime))

}
