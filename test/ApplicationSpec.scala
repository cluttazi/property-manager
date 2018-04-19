//package test
//
//import play.api.test.PlaySpecification
//import play.api.test.WithApplication
//import play.api.test.FakeRequest
//import play.api.Application
//
////todo fix random CSRF issue
//class ApplicationSpec extends PlaySpecification {
//
//  // -- Date helpers
//
//  def dateIs(date: java.util.Date, str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str
//
//  // --
//
//  "Application" should {
//
//    def applicationController(implicit app: Application) = {
//      val app2ApplicationController = Application.instanceCache[controllers.Application]
//      app2ApplicationController(app)
//    }
//
//    "redirect to the property list on /" in new WithApplication {
//      val result = applicationController.index(FakeRequest())
//
//      status(result) must equalTo(SEE_OTHER)
//      redirectLocation(result) must beSome.which(_ == "/properties")
//    }
//
//    "list properties on the the first page" in new WithApplication {
//      val result = applicationController.list(0, 2, "")(FakeRequest())
//
//      status(result) must equalTo(OK)
//      contentAsString(result) must contain("One property found")
//    }
//
//    "filter property by postcode" in new WithApplication {
//      val result = applicationController.list(0, 2, "12345")(FakeRequest())
//
//      status(result) must equalTo(OK)
//      contentAsString(result) must contain("One property found")
//
//    }
//
//    "create new property" in new WithApplication {
//      val badResult = applicationController.save(FakeRequest())
//
//      status(badResult) must equalTo(BAD_REQUEST)
//
//      val badDateFormat = applicationController.save(
//        FakeRequest().withFormUrlEncodedBody(
//          "address" -> "New Address",
//          "postcode" -> "54321",
//          "latitude" -> "34",
//          "longitude" -> "34",
//          "bedrooms" -> "2",
//          "surface" -> "3.4",
//          "price" -> "badbadbad")
//      )
//
//      status(badDateFormat) must equalTo(BAD_REQUEST)
//      contentAsString(badDateFormat) must contain("""<input type="text" id="price" name="price" value="badbadbad" />""")
//      contentAsString(badDateFormat) must contain("""<input type="text" id="name" name="name" value="New Address" />""")
//
//      val result = applicationController.save(
//        FakeRequest().withFormUrlEncodedBody(
//          "address" -> "New Address",
//          "postcode" -> "54321",
//          "latitude" -> "34",
//          "longitude" -> "34",
//          "bedrooms" -> "2",
//          "surface" -> "3.4",
//          "price" -> "2.0")
//      )
//
//      status(result) must equalTo(SEE_OTHER)
//      redirectLocation(result) must beSome.which(_ == "/properties")
//      flash(result).get("success") must beSome.which(_ == "property New Address has been created")
//
//      val list = applicationController.list(0, 2, "54321")(FakeRequest())
//
//      status(list) must equalTo(OK)
//      contentAsString(list) must contain("One property found")
//    }
//  }
//
//}