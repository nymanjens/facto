package hydro.common.testing

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

trait HookedSpecification extends Specification {

  override final def map(fragments: => Fragments): Fragments = {
    val fragmentsWithBeforeAndAfter = fragments flatMap (fragment => {
      val extendedFragments: Fragments = step(before) ^ fragment ^ step(after)
      extendedFragments.contents
    })
    step(beforeAll) ^ fragmentsWithBeforeAndAfter ^ step(afterAll)
  }

  protected def before() = {}
  protected def after() = {}
  protected def beforeAll() = {}
  protected def afterAll() = {}
}
