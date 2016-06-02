package tools

import scala.io.Source
import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import scala.util.matching.Regex

import java.nio.file.Path

import play.api.{Application, Logger}

import com.google.common.base.Splitter
import org.joda.time.DateTime
import models.SlickUtils.dbApi._
import org.apache.commons.lang.StringEscapeUtils

import models.SlickUtils.dbRun
import models.{User, Users}
import models.accounting.{BalanceCheck, Money, Transaction, TransactionGroup, UpdateLogs, UpdateLog, BalanceChecks, TransactionGroups, Transactions}
import models.accounting.config.Config

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
    for ((sqlInsert, i) <- sqlInserts.zipWithIndex) {
      if (i > 0 && i % 1000 == 0) {
        println(s"  Processed $i lines out of ${sqlInserts.size}")
      }
      sqlInsert match {
        case _: UserInsert => Unit
        case insert: TransactionOrBalanceCheckInsert => insert.doImport(originalUserIdToUser)
        case insert: LogInsert => insert.doImport(originalUserIdToUser)
      }
    }
  }

  private def defaultPassword(implicit app: Application): String = {
    app.configuration.getString("facto.setup.defaultPassword") getOrElse "changeme"
  }

  private def dateForMillisSinceEpoch(millis: Long): DateTime = {
    new DateTime(millis * 1000) plusHours 2 // +2 hours to correct for timezone problem in facto v1 (ugly hack)
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
          if (Config.moneyReservoirOption(moneyReservoirCode).isEmpty) {
            Logger.error(s"Found unknown moneyReservoirCode: $moneyReservoirCode")
          }

          if (categoryCode == "[BALANCE_SET]") {
            BalanceChecks.all.add(BalanceCheck(
              issuerId = issuer.id,
              moneyReservoirCode = moneyReservoirCode,
              balance = Money.fromFloat(price.toDouble),
              createdDate = dateForMillisSinceEpoch(creationTime.toLong),
              checkDate = dateForMillisSinceEpoch(datePayed.toLong)))

          } else {
            // Transaction
            val group = TransactionGroups.all.add(TransactionGroup(createdDate = dateForMillisSinceEpoch(creationTime.toLong)))
            Transactions.all.add(Transaction(
              transactionGroupId = group.id,
              issuerId = issuer.id,
              beneficiaryAccountCode = accountCode,
              moneyReservoirCode = moneyReservoirCode,
              categoryCode = categoryCode,
              description = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(description)),
              flow = Money.fromFloat(price.toDouble),
              createdDate = dateForMillisSinceEpoch(creationTime.toLong),
              transactionDate = dateForMillisSinceEpoch(datePayed.toLong),
              consumedDate = dateForMillisSinceEpoch(if (dateConsumed.toLong == 0) datePayed.toLong else dateConsumed.toLong)
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
          UpdateLogs.add(UpdateLog(
            userId = user.id,
            change = StringEscapeUtils.unescapeHtml(sql.replace("\\", "")),
            date = dateForMillisSinceEpoch(timestamp.toLong)
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
      Users.add(Users.newWithUnhashedPw(loginName, defaultPassword, name))
    }
  }
}
