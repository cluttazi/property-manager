package test

import dao.PricesHistoriesDAO
import play.api.Application
import play.api.test.CSRFTokenHelper._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Controller-level tests. The original version of this spec was disabled
  * because of "random CSRF issues": Play's CSRF filter rejects form POSTs
  * that carry no token. `withCSRFToken` (play.api.test.CSRFTokenHelper)
  * fakes a valid token per request, which makes these tests deterministic.
  */
class ApplicationSpec extends PlaySpecification {

  sequential

  private def applicationController(implicit app: Application) =
    Application.instanceCache[controllers.Application].apply(app)

  private def pricesHistoriesDao(implicit app: Application) =
    Application.instanceCache[PricesHistoriesDAO].apply(app)

  "Application" should {

    "redirect to the property list on /" in new WithApplication {
      val result = applicationController.index(FakeRequest())

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which((_: String).startsWith("/properties"))
    }

    "list properties on the first page" in new WithApplication {
      val result = applicationController.list(0, 2, "")(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("One property found")
    }

    "filter property by postcode" in new WithApplication {
      val result = applicationController.list(0, 2, "12345")(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("One property found")
    }

    "reject an invalid property form" in new WithApplication {
      val badResult = applicationController.save(FakeRequest().withCSRFToken)

      status(badResult) must equalTo(BAD_REQUEST)

      val badPrice = applicationController.save(
        FakeRequest()
          .withFormUrlEncodedBody(
            "address" -> "New Address",
            "postcode" -> "54321",
            "latitude" -> "34",
            "longitude" -> "34",
            "bedrooms" -> "2",
            "surface" -> "3.4",
            "price" -> "badbadbad")
          .withCSRFToken
      )

      status(badPrice) must equalTo(BAD_REQUEST)
      contentAsString(badPrice) must contain("""value="badbadbad"""")
    }

    "create a new property" in new WithApplication {
      val result = applicationController.save(
        FakeRequest()
          .withFormUrlEncodedBody(
            "address" -> "New Address",
            "postcode" -> "54321",
            "latitude" -> "34",
            "longitude" -> "34",
            "bedrooms" -> "2",
            "surface" -> "3.4",
            "price" -> "2.0")
          .withCSRFToken
      )

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which((_: String).startsWith("/properties"))
      flash(result).get("success") must beSome

      val list = applicationController.list(0, 2, "54321")(FakeRequest())

      status(list) must equalTo(OK)
      contentAsString(list) must contain("One property found")
    }

    // Regression test: `update` used to fire-and-forget the property update
    // and only sequence the price-history insert, so the redirect could win
    // the race against the writes. Both writes must be visible once the
    // response future has completed.
    "record a price history entry when a property is updated" in new WithApplication {
      val result = applicationController.update(1L)(
        FakeRequest()
          .withFormUrlEncodedBody(
            "address" -> "Updated Address",
            "postcode" -> "12345",
            "latitude" -> "34",
            "longitude" -> "34",
            "price" -> "123.45")
          .withCSRFToken
      )

      status(result) must equalTo(SEE_OTHER)

      val prices = Await.result(pricesHistoriesDao.findByProperty(1L), Duration.Inf)
      prices.size must equalTo(3) // 2 seeded + 1 from this update
      prices.map(_.price) must contain(123.45)
    }
  }
}
