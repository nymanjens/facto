package tools

import java.nio.file.Path
import java.time.Instant

import com.google.common.base.Splitter
import com.google.inject.Inject
import common.ResourceFiles
import common.money.Money
import common.time.{Clock, LocalDateTime, LocalDateTimes}
import models._
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.EntityModification
import models.user.User

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

final class CsvImportTool @Inject()(implicit clock: Clock, entityAccess: SlickEntityAccess) {

  def importTransactions(csvFilePath: Path)(implicit user: User): Unit = {
    // example of line: "2 :: Common :: LIFE :: CARD_COMMON :: imperdiet Duis  :: -25.04 :: 1425855600 :: 0 :: 1425934823"
    val lines = for (line <- ResourceFiles.readLines(csvFilePath) if !line.trim.isEmpty) yield line.trim
    for (line <- lines) {
      val parts = Splitter.on(" :: ").trimResults().split(line).asScala.toList
      parts match {
        case List(
            issuerId,
            beneficiaryAccountCode,
            categoryCode,
            moneyReservoirCode,
            description,
            flowAsFloat,
            transactionDateStamp,
            consumedDateStamp,
            createdDateStamp) =>
          val groupAddition =
            EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
          val group = groupAddition.entity
          val transactionAddition =
            EntityModification.createAddWithRandomId(
              Transaction(
                transactionGroupId = group.id,
                issuerId = issuerId.toInt,
                beneficiaryAccountCode = beneficiaryAccountCode,
                moneyReservoirCode = moneyReservoirCode,
                categoryCode = categoryCode,
                description = description,
                flowInCents = Money.floatToCents(flowAsFloat.toDouble),
                tags = Seq(s"csv-import-$beneficiaryAccountCode"),
                createdDate = epochSecondsToDateTime(createdDateStamp.toLong),
                transactionDate = epochSecondsToDateTime(transactionDateStamp.toLong),
                consumedDate = epochSecondsToDateTime(
                  if (consumedDateStamp.toLong == 0) transactionDateStamp.toLong
                  else consumedDateStamp.toLong)
              ))
          entityAccess.persistEntityModifications(groupAddition, transactionAddition)
      }
    }
  }

  def importBalanceChecks(csvFilePath: Path)(implicit user: User): Unit = {
    // example of line: "2 :: CASH_COMMON :: 40.58 :: 1426287600 :: 1426357095"
    val lines = for (line <- ResourceFiles.readLines(csvFilePath) if !line.trim.isEmpty) yield line.trim
    for (line <- lines) {
      val parts = Splitter.on(" :: ").trimResults().split(line).asScala.toList
      parts match {
        case List(issuerId, moneyReservoirCode, balanceAsFloat, checkDateStamp, createdDateStamp) =>
          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(BalanceCheck(
              issuerId = issuerId.toInt,
              moneyReservoirCode = moneyReservoirCode,
              balanceInCents = Money.floatToCents(balanceAsFloat.toDouble),
              createdDate = epochSecondsToDateTime(createdDateStamp.toLong),
              checkDate = epochSecondsToDateTime(checkDateStamp.toLong)
            )))
      }
    }
  }

  private def epochSecondsToDateTime(epochSeconds: Long): LocalDateTime = {
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zone = java.time.Clock.systemDefaultZone().getZone
    val javaDateTime = instant.atZone(zone).toLocalDateTime
    LocalDateTimes.ofJavaLocalDateTime(javaDateTime)
  }
}
