package hydro.models.access.webworker

import utest._

import scala.scalajs.js

object LocalDatabaseWebWorkerApiImplTest extends TestSuite {

  override def tests = TestSuite {
    "areEquivalentEntities" - {
      "empty dictionaries" - {
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(js.Dictionary(), js.Dictionary()) ==> true
      }

      "filters special keys" - {
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "meta" -> 5,
            "$loki" -> 6,
            "x" -> 7,
          ),
          fromClient = js.Dictionary(
            "x" -> 7,
          ),
        ) ==> true
      }

      "with primitives" - {
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "string" -> "abc",
            "int" -> 6,
            "float" -> 7.3,
          ),
          fromClient = js.Dictionary(
            "string" -> "abc",
            "int" -> 6,
            "float" -> 7.3,
          ),
        ) ==> true
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "string" -> "abc",
            "int" -> 6,
            "float" -> 7.3,
          ),
          fromClient = js.Dictionary(
            "string" -> "abc",
            "int" -> 6,
            "float" -> 7.4,
          ),
        ) ==> false
      }

      "with nested dictionaries" - {
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "nested" -> js.Dictionary(
              "x" -> 7,
            ),
          ),
          fromClient = js.Dictionary(
            "nested" -> js.Dictionary(
              "x" -> 7,
            ),
          ),
        ) ==> true
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "nested" -> js.Dictionary(
              "x" -> 7,
            ),
          ),
          fromClient = js.Dictionary(
            "nested" -> js.Dictionary(
              "x" -> 8,
            ),
          ),
        ) ==> false
      }

      "with arrays" - {
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "array" -> js.Array(1, 2, "3"),
          ),
          fromClient = js.Dictionary(
            "array" -> js.Array(1, 2, "3"),
          ),
        ) ==> true
        LocalDatabaseWebWorkerApiImpl.areEquivalentEntities(
          fromLoki = js.Dictionary(
            "array" -> js.Array(1, 2, "3"),
          ),
          fromClient = js.Dictionary(
            "array" -> js.Array(1, 2, 3),
          ),
        ) ==> false
      }
    }
  }
}
