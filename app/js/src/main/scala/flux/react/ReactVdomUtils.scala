package flux.react

import japgolly.scalajs.react.ReactElement
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

object ReactVdomUtils {
  object ^^ {
    def classes(cls: String*): TagMod = classes(cls.toVector)
    def classes(cls: Seq[String]): TagMod = ^.classSetM(cls.map(c => (c, true)).toMap)

    def ifThen(cond: Boolean)(thenElement: => TagMod): TagMod = {
      if(cond) {
        thenElement
      } else {
        Seq()
      }
    }
  }
}
