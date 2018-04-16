package test

import dao.{PricesHistoriesDAO, PropertiesDAO}
import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplication

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ModelSpec extends Specification {

  // -- Date helpers

  def dateIs(date: java.util.Date, str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str

  // --

  "Property model" should {

    def pricesHistoriesDao(implicit app: Application) = {
      val app2PricesHistoriesDAO = Application.instanceCache[PricesHistoriesDAO]
      app2PricesHistoriesDAO(app)
    }

    def propertiesDao(implicit app: Application) = {
      val app2PropertiesDAO = Application.instanceCache[PropertiesDAO]
      app2PropertiesDAO(app)
    }

    "be retrieved by id" in new WithApplication {
      val macintosh = Await.result(propertiesDao.findById(21), Duration.Inf).get
      macintosh.name must equalTo("Macintosh")
      macintosh.introduced must beSome.which(dateIs(_, "1984-01-24"))
    }

    "be listed along its companies" in new WithApplication {
      val computers = Await.result(propertiesDao.list(), Duration.Inf)
      computers.total must equalTo(574)
      computers.items must have length (10)
    }

    "be updated if needed" in new WithApplication {
      Await.result(propertiesDao.update(21, Computer(name = "The Macintosh", introduced = None, discontinued = None, companyId = Some(1))), Duration.Inf)

      val macintosh = Await.result(propertiesDao.findById(21), Duration.Inf).get
      macintosh.name must equalTo("The Macintosh")
      macintosh.introduced must beNone

    }

  }

}