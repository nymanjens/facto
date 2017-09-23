package models.accounting

import com.google.common.collect.ImmutableMultiset
import common.CollectionUtils.toListMap
import common.accounting.Tags
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.dbRun
import models.accounting.SlickTagEntityManager.{TagEntities, tableName}
import models.manager.{EntityTable, ImmutableEntityManager, SlickEntityManager}

import scala.collection.JavaConverters._
import scala.collection.immutable.{ListMap, Seq}

final class SlickTagEntityManager
    extends ImmutableEntityManager[TagEntity, TagEntities](
      SlickEntityManager.create[TagEntity, TagEntities](
        tag => new TagEntities(tag),
        tableName = tableName
      ))
    with TagEntity.Manager {

  // Validates the tag name at creation time
  override def add(tagEntity: TagEntity): TagEntity = {
    Tags.isValidTag(tagEntity.name)
    super.add(tagEntity)
  }

  override def usageAndReccomendations(selectedTags: Seq[String],
                                       maxNumRecommendations: Int): ListMap[String, Int] = {
    val tagToUsagePairs: Seq[(String, Int)] = {
      val allTagNames = dbRun(newQuery.map(_.name))
      val multiset = ImmutableMultiset.copyOf(allTagNames.asJava)
      for (tagName <- multiset.elementSet.asScala.toList) yield {
        tagName -> multiset.count(tagName)
      }
    }
    val tagToUsageMap: Map[String, Int] = tagToUsagePairs.toMap withDefault (_ => 0)

    val selectedTagsMap: ListMap[String, Int] = {
      toListMap(for (tag <- selectedTags) yield {
        (tag, tagToUsageMap(tag))
      })
    }
    val recommendedTagsMap: ListMap[String, Int] = {
      val numRecommendations = maxNumRecommendations - selectedTags.size
      if (numRecommendations <= 0) {
        toListMap(Seq())
      } else {
        toListMap(
          tagToUsagePairs
            .sortBy(-_._2)
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
