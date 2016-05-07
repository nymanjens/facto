package models.activeslick

import models.SlickUtils.dbApi._
import scala.language.implicitConversions
import scala.util.{Success, Failure, Try}
import models.SlickUtils.dbRun

class EntityTableQuery[M <: Identifiable[M], T <: EntityTable[M]](cons: Tag => T) extends TableQuery(cons) {

  def count: Int = dbRun(this.length.result)

  def fetchAll: Seq[M] = dbRun(this.result)

  def save(model: M): M = trySave(model).get

  def update(model: M): M = tryUpdate(model).get

  def delete(model: M): Unit = tryDelete(model).get

  def tryExtractId(model: M): Try[Long] = {
    model.id match {
      case Some(id) => Success(id)
      case None => Failure(RowNotFoundException(model))
    }
  }

  protected def filterById(id: Long) = filter(_.id === id)

  /**
    * Define an insert query that returns the database generated identifier.
    * @param model a mapped model
    * @return the database generated identifier.
    */
  def add(model: M): Long = tryAdd(model).get

  def tryAdd(model: M): Try[Long] = {
    Try(dbRun(this.returning(this.map(_.id)).+=(model)))
  }

  protected def mustAffectOneSingleRow(query: => Int): Try[Unit] = {

    val affectedRows = query

    if (affectedRows == 1) {
      Success(Unit)
    } else if (affectedRows == 0) {
      Failure(NoRowsAffectedException)
    } else {
      Failure(ManyRowsAffectedException(affectedRows))
    }

  }

  /**
    * Try to update the model.
    * @return A Success[M] is case of success, Failure otherwise.
    */
  def tryUpdate(model: M): Try[M] = {
    tryExtractId(model).flatMap { id =>
      tryUpdate(id, model)
    }
  }

  /**
    * Try to save the model.
    * @return A Success[M] is case of success, Failure otherwise.
    */
  def trySave(model: M): Try[M] = {
    model.id match {
      // if has an Id, try to update it
      case Some(id) => tryUpdate(id, model)

      // if has no Id, try to add it
      case None => tryAdd(model).map { id => model.withId(id) }
    }
  }

  protected def tryUpdate(id: Long, model: M): Try[M] = {
    mustAffectOneSingleRow {
      dbRun(filterById(id).update(model))
    }.recoverWith {
      // if nothing gets updated, we want a Failure[RowNotFoundException]
      // all other failures must be propagated
      case NoRowsAffectedException => Failure(RowNotFoundException(model))

    }.map { _ =>
      model // return a Try[M] if only one row is affected
    }
  }

  /**
    * Try to delete the model.
    * @return A Success[Unit] is case of success, Failure otherwise.
    */
  def tryDelete(model: M): Try[Unit] = {
    tryExtractId(model).flatMap { id =>
      tryDeleteById(id)
    }
  }

  def deleteById(id: Long): Unit = tryDeleteById(id).get

  def tryDeleteById(id: Long): Try[Unit] = {
    mustAffectOneSingleRow {
      dbRun(filterById(id).delete)

    }.recoverWith {
      // if nothing gets deleted, we want a Failure[RowNotFoundException]
      // all other failures must be propagated
      case NoRowsAffectedException => Failure(RowNotFoundException(id))
    }
  }

  def tryFindById(id: Long): Try[M] = {
    findOptionById(id) match {
      case Some(model) => Success(model)
      case None => Failure(RowNotFoundException(id))
    }
  }

  def findById(id: Long): M = findOptionById(id).get

  def findOptionById(id: Long): Option[M] = {
    dbRun(filterById(id).result) match {
      case Seq(x) => Some(x)
      case Seq() => None
    }
  }
}
