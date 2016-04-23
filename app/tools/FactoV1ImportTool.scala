package tools

import scala.io.Source
import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import scala.util.matching.Regex

import java.nio.file.Path

import play.api.Application

import com.google.common.base.Splitter
import org.joda.time.DateTime
import slick.driver.H2Driver.api._
import org.apache.commons.lang.StringEscapeUtils

import models.ModelUtils.dbRun
import models.{User, Users}
import models.accounting.{BalanceCheck, Money, Transaction, TransactionGroup, UpdateLogs, UpdateLog, BalanceChecks, TransactionGroups, Transactions}

object FactoV1ImportTool {

  def importFromSqlDump(sqlFilePath: Path)(implicit app: Application): Unit = {
    val sqlInserts: Seq[SqlInsert] = {
      for {
        line <- Source.fromFile(sqlFilePath.toFile, "UTF-8").getLines()
        if (line startsWith "INSERT INTO ")
      } yield {
        // Ignore all lines except those starting with "INSTERT INTO"
        val lineRegex =
          """^INSERT INTO (\S+) VALUES\((.+)\);$""".r
        line match {
          case lineRegex("facto_account_inputs", values) =>
            TransactionOrBalanceCheckInsert(values)
          case lineRegex("facto_logs", values) =>
            LogInsert(values)
          case lineRegex("facto_users", values) =>
            UserInsert(values)
        }
      }
    }.toList

    // Insert users - needs to be done first because Map[originalId -> User] is needed for other inserts.
    val userInserts: Seq[UserInsert] = sqlInserts.collect { case insert: UserInsert => insert }
    val originalUserIdToUser: Map[Long, User] = {
      for (userInsert <- userInserts) yield {
        userInsert.originalUserId -> userInsert.doImport()
      }
    }.toMap

    // Insert rest
    for (sqlInsert <- sqlInserts) {
      sqlInsert match {
        case _: UserInsert => Unit
        case insert: TransactionOrBalanceCheckInsert => insert.doImport(originalUserIdToUser)
        case insert: LogInsert => insert.doImport(originalUserIdToUser)
      }
    }
  }

  private def defaultPassword(implicit app: Application): String = {
    app.configuration.getString("facto.import.defaultPassword") getOrElse "changeme"
  }

  sealed abstract class SqlInsert(values: String)

  case class TransactionOrBalanceCheckInsert(private val values: String) extends SqlInsert(values) {

    def doImport(originalUserIdToUser: Map[Long, User]): Unit = {
      // Fields: id, issuer, account, category, payed_with_what, payed_with_whose, description, price, date_payed, date_consumed, creation_time
      // Example: "8","14","Bob","FUN","CASH","Bob","Takeaway Pizza","-11.5","1451602800","1458514800","1451666823"
      val valuesRegex =
        """^"\d+","(\d+)","([^"]*)","([^"]+)","([^"]+)","([^"]+)","(.*?)","([^"]+)","(\d+)","(\d+)","(\d+)"$""".r
      values match {
        case valuesRegex(issuerOriginalId, accountCode, categoryCode, payedWithWhatCode, payedWithWhoseCode, description, price, datePayed, dateConsumed, creationTime) =>
          val issuer = originalUserIdToUser(issuerOriginalId.toLong)
          val moneyReservoirCode = s"${payedWithWhatCode}_${payedWithWhoseCode.toUpperCase}"

          if (categoryCode == "[BALANCE_SET]") {
            BalanceChecks.all.save(BalanceCheck(
              issuerId = issuer.id.get,
              moneyReservoirCode = moneyReservoirCode,
              balance = Money.fromFloat(price.toDouble),
              createdDate = new DateTime(creationTime.toLong * 1000),
              checkDate = new DateTime(datePayed.toLong * 1000)))

          } else {
            // Transaction
            val group = TransactionGroups.all.save(TransactionGroup(createdDate = new DateTime(creationTime.toLong * 1000)))
            Transactions.all.save(Transaction(
              transactionGroupId = group.id.get,
              issuerId = issuer.id.get,
              beneficiaryAccountCode = accountCode,
              moneyReservoirCode = moneyReservoirCode,
              categoryCode = categoryCode,
              description = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(description)),
              flow = Money.fromFloat(price.toDouble),
              createdDate = new DateTime(creationTime.toLong * 1000),
              transactionDate = new DateTime(datePayed.toLong * 1000),
              consumedDate = new DateTime(if (dateConsumed.toLong == 0) datePayed.toLong * 1000 else dateConsumed.toLong * 1000)
            ))
          }
      }
    }
  }

  case class LogInsert(private val values: String) extends SqlInsert(values) {
    def doImport(originalUserIdToUser: Map[Long, User]): Unit = {
      // Fields: id, timestamp, user_id, sql
      // Example: INSERT INTO facto_logs VALUES("1","1313234891","14","INSERT INTO facto_account_inputs...");
      val valuesRegex =
        """^"\d+","(\d+)","(\d+)","([^"]+)"$""".r

      values match {
        case valuesRegex(timestamp, originalUserId, sql) =>
          val user = originalUserIdToUser(originalUserId.toLong)
          UpdateLogs.all.save(UpdateLog(
            userId = user.id.get,
            change = StringEscapeUtils.unescapeHtml(sql.replace("\\", "")),
            date = new DateTime(timestamp.toLong * 1000)
          ))
      }
    }
  }

  case class UserInsert(private val values: String) extends SqlInsert(values) {
    // Fields: id, login_name, password, name, last_visit
    // Example: "14","bob","97a156...caab666","Bob","1460811765"
    private val valuesRegex =
      """^"(\d+)","(\w+)","\w+","(\w+)","\d+"$""".r

    val (originalUserId, loginName, name): (Long, String, String) =
      values match {
        case valuesRegex(originalUserId, loginName, name) => (originalUserId.toLong, loginName, name)
      }

    def doImport()(implicit app: Application): User = {
      Users.all.save(Users.newWithUnhashedPw(loginName, defaultPassword, name))
    }
  }
}
