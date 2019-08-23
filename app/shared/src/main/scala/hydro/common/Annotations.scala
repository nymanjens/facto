package hydro.common

import scala.annotation.StaticAnnotation

object Annotations {

  /** Scala version of com.google.common.annotations.VisibleForTesting. */
  class visibleForTesting extends StaticAnnotation

  /** Scala version of javax.annotations.Nullable. */
  class nullable extends StaticAnnotation

  /** Scala version of GuardedBy. */
  class guardedBy(objectName: String) extends StaticAnnotation
}
