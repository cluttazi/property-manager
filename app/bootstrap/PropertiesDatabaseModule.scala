package bootstrap

import com.google.inject.AbstractModule

class PropertiesDatabaseModule extends AbstractModule {
  override protected def configure(): Unit = {
    bind(classOf[InitialData]).asEagerSingleton()
  }
}
