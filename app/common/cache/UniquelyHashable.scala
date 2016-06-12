package common.cache

import scala.collection.immutable.Seq
import scala.collection.mutable

import java.util.function.BiConsumer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.Callable

import org.joda.time.Duration
import org.apache.http.annotation.GuardedBy
import com.google.common.cache.{Cache, CacheBuilder, LoadingCache, CacheLoader}

trait UniquelyHashable extends Object {
  def uniqueHash: String
}
