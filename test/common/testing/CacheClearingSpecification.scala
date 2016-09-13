package common.testing

import common.cache.CacheRegistry

trait CacheClearingSpecification extends HookedSpecification {

  override final def before() = {
    CacheRegistry.resetCachesForTests()
    beforeEveryTest()
  }

  override final def after() = afterEveryTest()

  protected def beforeEveryTest() = {}
  protected def afterEveryTest() = {}
}
