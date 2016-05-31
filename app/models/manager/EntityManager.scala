package models.manager

import scala.collection.immutable.Seq

import models.activeslick.Identifiable
import models.SlickUtils.dbApi._

trait EntityManager[E <: Identifiable[E]] {
  def add(e: E): E
  def update(e: E): E
  def delete(e: E): Unit

  def findById(id: Long): E
  def fetchAll(selection: Stream[E] => Stream[E] = s => s): Seq[E]

  def schema: slickDriver.profile.SchemaDescription
}
