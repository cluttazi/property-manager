package bootstrap

import com.google.inject.AbstractModule

class PropertiesDatabaseModule extends AbstractModule {
  protected def configure: Unit = {
    bind(classOf[InitialData]).asEagerSingleton()
  }
}
