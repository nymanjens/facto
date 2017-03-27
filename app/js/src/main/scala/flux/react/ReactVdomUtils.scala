package flux.react

import japgolly.scalajs.react.{ReactElement, ReactNode}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

object ReactVdomUtils {
  object ^^ {
    def classes(cls: String*): TagMod = classes(cls.toVector)
    def classes(cls: Seq[String]): TagMod = ^.classSetM(cls.filter(_.nonEmpty).map(c => (c, true)).toMap)

    def ifThen(cond: Boolean)(thenElement: => TagMod): TagMod = {
      if (cond) {
        thenElement
      } else {
        Seq()
      }
    }
    def ifThen[T](option: Option[T])(thenElement: T => TagMod): TagMod = {
      ifThen(option.isDefined)(thenElement(option.get))
    }
  }

  object << {
    def ifThen(cond: Boolean)(thenElement: => ReactNode): ReactNode = {
      if (cond) {
        thenElement
      } else {
        Seq()
      }
    }

    def ifThen[T](option: Option[T])(thenElement: T => ReactNode): ReactNode = {
      ifThen(option.isDefined)(thenElement(option.get))
    }
  }
}
