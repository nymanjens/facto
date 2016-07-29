package models.accounting

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.CollectionUtils.toListMap
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.manager.{Entity, EntityManager, EntityTable, ImmutableEntityManager}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

/**
  * Notes when using TagEntities:
  * - TagEntities should only be updated by the Transactions EntityManager!
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

  /**
    * Returns the selected tags augmented with recommendations.
    *
    * @param maxNumRecommendations If selectedTags has a size smaller than this, recommendations will fill up the
    *                              response until the size is equal to this.
    * @return an map with a fixed iteration order (most used first), mapping the Tag to the number of transactions
    *         it is used in
    */
  def usageAndReccomendations(selectedTags: Seq[Tag], maxNumRecommendations: Int): ListMap[Tag, Int] = {
    val tagToUsagePairs: Seq[(Tag, Int)] = {
      val allTagNames = dbRun(newQuery.map(_.name))
      val multiset = ImmutableMultiset.copyOf(allTagNames.asJava)
      for (tagName <- multiset.elementSet.asScala.toList) yield {
        Tag(tagName) -> multiset.count(tagName)
      }
    }
    val tagToUsageMap: Map[Tag, Int] = tagToUsagePairs.toMap

    val selectedTagsMap: ListMap[Tag, Int] = {
      toListMap(
        for (tag <- selectedTags) yield {
          (tag, tagToUsageMap(tag))
        })
    }
    val recommendedTagsMap: ListMap[Tag, Int] = {
      val numRecommendations = maxNumRecommendations - selectedTags.size
      if (numRecommendations <= 0) {
        toListMap(Seq())
      } else {
        toListMap(tagToUsagePairs.sortBy(-_._2).take(numRecommendations))
      }
    }

    selectedTagsMap ++ recommendedTagsMap
  }
}
