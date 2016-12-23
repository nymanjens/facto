package flux

final class Dispatcher {

  def register(callback: Action => Unit): Unit = {

  }

  def dispatch(action: Action): Unit = {

  }

  private def isDispatching(): Boolean = {
    false // TODO
  }
}
