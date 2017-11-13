package flux.react

import japgolly.scalajs.react.vdom.{TagMod, VdomArray, VdomNode}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.Implicits._

import scala.collection.immutable.Seq

object ReactVdomUtils {
  object ^^ {
    def classes(cls: String*): TagMod = classes(cls.toVector)
    def classes(cls: Seq[String]): TagMod = ^.classSetM(cls.filter(_.nonEmpty).map(c => (c, true)).toMap)

    def ifThen(cond: Boolean)(thenElement: => TagMod): TagMod = {
      if (cond) {
        thenElement
      } else {
        EmptyVdom
      }
    }
    def ifThen[T](option: Option[T])(thenElement: T => TagMod): TagMod = {
      ifThen(option.isDefined)(thenElement(option.get))
    }
  }

  object << {
    def ifThen(cond: Boolean)(thenElement: => VdomNode): VdomNode = {
      if (cond) {
        thenElement
      } else {
        VdomArray.empty()
      }
    }

    def ifThen[T](option: Option[T])(thenElement: T => VdomNode): VdomNode = {
      ifThen(option.isDefined)(thenElement(option.get))
    }

    def joinWithSpaces[A](elems: TraversableOnce[A])(implicit f: A => VdomNode,
                                                     stringF: String => VdomNode): VdomArray = {
      VdomArray.empty() ++= elems.flatMap(a => Seq(f(a), stringF(" ")))
    }
  }
}
