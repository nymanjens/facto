package models.manager

import scala.collection.immutable.Seq

trait EntityManager[E <: Identifiable[E]] {

  // ********** Management methods ********** //
  def initialize(): Unit = {}
  def verifyConsistency(): Unit = {}
  def createSchema: Unit
  def tableName: String

  // ********** Mutators ********** //
  def add(entity: E): E
  def update(entity: E): E
  def delete(entity: E): Unit

  // ********** Getters ********** //
  def findById(id: Long): E
  def fetchAll(selection: Stream[E] => Stream[E] = s => s): Seq[E]
}
