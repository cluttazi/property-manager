package test

import java.util.Date

import dao.{PricesHistoriesDAO, PropertiesDAO}
import models.{PriceHistory, Property}
import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplication
import utils.Enums

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ModelSpec extends Specification {

  // -- Date helpers

  def dateIs(date: java.util.Date, str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str

  // --

  "Property model" should {

    def pricesHistoriesDao(implicit app: Application) = {
      lazy val app2PricesHistoriesDAO = Application.instanceCache[PricesHistoriesDAO]
      app2PricesHistoriesDAO(app)
    }

    def propertiesDao(implicit app: Application) = {
      lazy val app2PropertiesDAO = Application.instanceCache[PropertiesDAO]
      app2PropertiesDAO(app)
    }

    "be retrieved by id" in new WithApplication {
      lazy val property = Await.result(propertiesDao.findById(1L), Duration.Inf).get
      property.address must equalTo("One Address")
      property.postcode must equalTo("12345")
      property.latitude must equalTo(33.00)
      property.longitude must equalTo(33.00)
      property.bedrooms must equalTo(None)
      property.surface must equalTo(None)
      property.price must equalTo(100.0)
    }

    "have prices histories" in new WithApplication {
      lazy val price = Await.result(pricesHistoriesDao.findByProperty(1L), Duration.Inf)
      price.size must equalTo(2)
    }

    "be updated if needed, and change price history" in new WithApplication {
      Await.result(propertiesDao.update(1L, Property(
        address = "New Address",
        postcode = "54321",
        latitude = 34.00,
        longitude = 34.00,
        bedrooms = Some(2),
        surface = Some(3.4),
        price = 2.0)), Duration.Inf)

      lazy val updated = Await.result(propertiesDao.findById(1L), Duration.Inf).get
      updated.address must equalTo("New Address")
      updated.postcode must equalTo("54321")
      updated.latitude must equalTo(34.00)
      updated.longitude must equalTo(34.00)
      updated.bedrooms must equalTo(Some(2))
      updated.surface must equalTo(Some(3.0))
      updated.price must equalTo(2.0)

      pricesHistoriesDao.insert(PriceHistory(Some(Enums.emptyLong), updated.id, System.currentTimeMillis, updated.price))

      lazy val price = Await.result(pricesHistoriesDao.findByProperty(1L), Duration.Inf)
      price.size must equalTo(3)

    }

  }

}