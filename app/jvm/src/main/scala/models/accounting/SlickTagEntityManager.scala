package models.accounting

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.CollectionUtils.toListMap
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

import SlickTagEntityManager.{TagEntities, tableName}

final class SlickTagEntityManager extends ImmutableEntityManager[TagEntity, TagEntities](
  SlickEntityManager.create[TagEntity, TagEntities](
    tag => new TagEntities(tag),
    tableName = tableName
  )) with TagEntity.Manager {

  // Validates the tag name at creation time
  override def add(tagEntity: TagEntity): TagEntity = {
    Tag.isValidTagName(tagEntity.name)
    super.add(tagEntity)
  }

  override def usageAndReccomendations(selectedTags: Seq[Tag], maxNumRecommendations: Int): ListMap[Tag, Int] = {
    val tagToUsagePairs: Seq[(Tag, Int)] = {
      val allTagNames = dbRun(newQuery.map(_.name))
      val multiset = ImmutableMultiset.copyOf(allTagNames.asJava)
      for (tagName <- multiset.elementSet.asScala.toList) yield {
        Tag(tagName) -> multiset.count(tagName)
      }
    }
    val tagToUsageMap: Map[Tag, Int] = tagToUsagePairs.toMap withDefault (_ => 0)

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
        toListMap(
          tagToUsagePairs.sortBy(-_._2)
            .filter { case (tag, usage) => !selectedTagsMap.contains(tag) }
            .take(numRecommendations))
      }
    }

    selectedTagsMap ++ recommendedTagsMap
  }
}

object SlickTagEntityManager {
  private val tableName: String = "TAG_ENTITIES"

  final class TagEntities(tag: SlickTag) extends EntityTable[TagEntity](tag, tableName) {
    def name = column[String]("name")
    def transactionId = column[Long]("transactionId")

    override def * = (name, transactionId, id.?) <> (TagEntity.tupled, TagEntity.unapply)
  }
}

