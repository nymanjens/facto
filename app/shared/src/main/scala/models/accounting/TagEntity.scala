package models.accounting

import models.manager.{Entity, EntityManager}
import models.EntityAccess

import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

/**
  * Notes when using TagEntities:
  * - TagEntities should only be updated by the Transaction.Manager
  * - TagEntities are immutable. Just delete and create a new one when updating.
  */
case class TagEntity(name: String,
                     transactionId: Long,
                     idOption: Option[Long] = None) extends Entity[TagEntity] {
  require(!name.isEmpty)
  require(transactionId > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def tag: Tag = Tag(name)
  def transaction(implicit entityAccess: EntityAccess): Transaction =
    entityAccess.transactionManager.findById(transactionId)

  override def toString = {
    s"TagEntity($name, transactionId=$transactionId)"
  }
}
object TagEntity {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[TagEntity] {

    /**
      * Returns the selected tags augmented with recommendations.
      *
      * @param maxNumRecommendations If selectedTags has a size smaller than this, recommendations will fill up the
      *                              response until the size is equal to this.
      * @return an map with a fixed iteration order (most used first), mapping the Tag to the number of transactions
      *         it is used in
      */
    def usageAndReccomendations(selectedTags: Seq[Tag], maxNumRecommendations: Int): ListMap[Tag, Int]
  }
}
