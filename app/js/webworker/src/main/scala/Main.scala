import hydro.models.access.webworker.LocalDatabaseWebWorkerScript

object Main {
  def main(args: Array[String]): Unit = {
    LocalDatabaseWebWorkerScript.run()
  }
}
