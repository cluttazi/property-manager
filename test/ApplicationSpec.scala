package test

import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.api.test.FakeRequest
import play.api.Application

class ApplicationSpec extends PlaySpecification {

  // -- Date helpers

  def dateIs(date: java.util.Date, str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str

  // --

  "Application" should {

    def applicationController(implicit app: Application) = {
      val app2ApplicationController = Application.instanceCache[controllers.Application]
      app2ApplicationController(app)
    }

    "redirect to the property list on /" in new WithApplication {
      val result = applicationController.index(FakeRequest())

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/properties")
    }

    "list properties on the the first page" in new WithApplication {
      val result = applicationController.list(0, 2, "")(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("One property found")
    }

    "filter property by postcode" in new WithApplication {
      val result = applicationController.list(0, 2, "12345")(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("One property found")

    }

    "create new property" in new WithApplication {
      val badResult = applicationController.save(FakeRequest())

      status(badResult) must equalTo(BAD_REQUEST)

      val badDateFormat = applicationController.save(
        FakeRequest().withFormUrlEncodedBody(
          "name" -> "FooBar",
          "introduced" -> "badbadbad",
          "company" -> "1")
      )

      status(badDateFormat) must equalTo(BAD_REQUEST)
      contentAsString(badDateFormat) must contain("""<option value="1" selected="selected">Apple Inc.</option>""")
      contentAsString(badDateFormat) must contain("""<input type="text" id="introduced" name="introduced" value="badbadbad" />""")
      contentAsString(badDateFormat) must contain("""<input type="text" id="name" name="name" value="FooBar" />""")

      val result = applicationController.save(
        FakeRequest().withFormUrlEncodedBody(
          "name" -> "FooBar",
          "introduced" -> "2011-12-24",
          "company" -> "1")
      )

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/properties")
      flash(result).get("success") must beSome.which(_ == "property FooBar has been created")

      val list = applicationController.list(0, 2, "FooBar")(FakeRequest())

      status(list) must equalTo(OK)
      contentAsString(list) must contain("One property found")
    }
  }

}