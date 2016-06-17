package models.accounting

import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.manager.{Entity, EntityManager, EntityTable, ImmutableEntityManager}

/**
  * Notes when using TagEntities:
  *   - TagEntities should only be updated by the Transactions EntityManager!
  *   - TagEntities are immutable. Just delete and create a new one when updating.
  */
case class TagEntity(name: String,
                     transactionId: Long,
                     idOption: Option[Long] = None) extends Entity[TagEntity] {
  require(!name.isEmpty)
  require(transactionId > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def tag: Tag = Tag(name)
  lazy val transaction: Transaction = Transactions.findById(transactionId)

  override def toString = {
    s"TagEntity($name, transactionId=$transactionId)"
  }
}

class TagEntities(tag: SlickTag) extends EntityTable[TagEntity](tag, TagEntities.tableName) {
  def name = column[String]("name")
  def transactionId = column[Long]("transactionId")

  override def * = (name, transactionId, id.?) <>(TagEntity.tupled, TagEntity.unapply)
}

object TagEntities extends ImmutableEntityManager[TagEntity, TagEntities](
  EntityManager.create[TagEntity, TagEntities](
    tag => new TagEntities(tag), tableName = "TAG_ENTITIES")) {

  // Validates the tag name at creation time
  override def add(tagEntity: TagEntity): TagEntity = {
    Tag.isValidTagName(tagEntity.name)
    super.add(tagEntity)
  }
}
