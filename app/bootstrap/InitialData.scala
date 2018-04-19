package bootstrap

import java.text.SimpleDateFormat
import java.util.Date

import dao.{PricesHistoriesDAO, PropertiesDAO}
import javax.inject.Inject
import models.{PriceHistory, Property}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

/** Initial set of data to be imported into the application. */
private[bootstrap] class InitialData @Inject()(propertiesDAO: PropertiesDAO,
                                               pricesHistoriesDAO: PricesHistoriesDAO
                                              )(implicit executionContext: ExecutionContext) {

  def insert(): Unit = {
    val insertInitialDataFuture = for {
      count <- propertiesDAO.count() if count == 0
      _ <- pricesHistoriesDAO.insert(InitialData.pricesHistories1)
      _ <- pricesHistoriesDAO.insert(InitialData.pricesHistories2)
      _ <- propertiesDAO.insert(InitialData.properties)
    } yield ()

    Try(Await.result(insertInitialDataFuture, Duration.Inf))
  }

  insert()
}

private[bootstrap] object InitialData {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  def pricesHistories1 = PriceHistory(Option(0L), Option(1L), System.currentTimeMillis, 100.0)

  def pricesHistories2 = PriceHistory(Option(0L), Option(1L), System.currentTimeMillis, 100.0)

  def properties = Property(Option(1L), "One Address", "12345",
    33.00, 33.00, None, None, 100.0)
}
