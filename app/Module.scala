import com.google.inject.AbstractModule
import tools.ApplicationStartHook

class Module extends AbstractModule {
  def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton
  }
}