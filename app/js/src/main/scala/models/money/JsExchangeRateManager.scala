package models.money

import common.money.{Currency, ExchangeRateManager}
import common.time.LocalDateTime
import models.access.DbQueryImplicits._
import models.access.RemoteDatabaseProxy
import models.modification.{EntityModification, EntityType}

import scala.collection.SortedMap
import scala.collection.immutable.Seq
import scala2js.Converters._
import models.access.Fields

final class JsExchangeRateManager(
    ratioReferenceToForeignCurrency: Map[Currency, SortedMap[LocalDateTime, Double]])(
    implicit database: RemoteDatabaseProxy)
    extends ExchangeRateManager {
  database.registerListener(RemoteDatabaseProxyListener)

  private var measurementsCache: Map[Currency, SortedMap[LocalDateTime, Double]] =
    ratioReferenceToForeignCurrency

  // **************** Implementation of ExchangeRateManager trait ****************//
  override def getRatioSecondToFirstCurrency(firstCurrency: Currency,
                                             secondCurrency: Currency,
                                             date: LocalDateTime): Double = {
    (firstCurrency, secondCurrency) match {
      case (Currency.default, Currency.default) => 1.0
      case (foreignCurrency, Currency.default) =>
        ratioReferenceToForeignCurrency(foreignCurrency, date)
      case (Currency.default, foreignCurrency) =>
        1 / getRatioSecondToFirstCurrency(secondCurrency, firstCurrency, date)
      case _ =>
        throw new UnsupportedOperationException(
          s"Exchanging from non-reference to non-reference currency is not " +
            s"supported ($firstCurrency -> $secondCurrency)")
    }
  }

  // **************** Private helper methods ****************//
  private def ratioReferenceToForeignCurrency(currency: Currency, date: LocalDateTime): Double = {
    measurementsCache(currency).to(date).lastOption match {
      case Some((lastDate, lastRatio)) => lastRatio
      case None => 1.0
    }
  }

  // **************** Inner type definitions ****************//
  private object RemoteDatabaseProxyListener extends RemoteDatabaseProxy.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    override def addedRemotely(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    private def addedModifications(modifications: Seq[EntityModification]): Unit = {
      for (modification <- modifications) {
        modification.entityType match {
          case EntityType.ExchangeRateMeasurementType =>
            modification match {
              case EntityModification.Add(_) =>
              // This happens infrequently
              // TODO: Update cache
              case EntityModification.Update(_) =>
                throw new UnsupportedOperationException("Immutable entity")
              case EntityModification.Remove(_) =>
              // Measurements are normally not removed
              // TODO: Update cache
            }
          case _ => false // do nothing
        }
      }
    }
  }
}
