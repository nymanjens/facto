package common

import com.google.common.base.Function

object Require {

  def requireNonNullFields(caseClassInstance: Product): Unit = {
    val paramsAsMap: Map[String, Any] = {
      // see http://stackoverflow.com/a/1227643/1218058
      (Map[String, Any]() /: caseClassInstance.getClass.getDeclaredFields) { (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(caseClassInstance))
      }
    }

    for ((fieldName, value) <- paramsAsMap) {
      require(value != null, s"Value for field '$fieldName' may not be null.")
    }
  }
}
