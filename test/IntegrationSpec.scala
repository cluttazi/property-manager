package test

import org.fluentlenium.core.filter.FilterConstructor.withText
import org.specs2.mutable.Specification

import play.api.test.Helpers.HTMLUNIT
import play.api.test.Helpers.running
import play.api.test.TestServer

class IntegrationSpec extends Specification {

  "Application" should {

    "work from within a browser" in {
      val port = 3335
      running(TestServer(port), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:" + port)

        browser.$("header h1").first.text() must equalTo("Property Manager")
        browser.$("section h1").first.text() must equalTo("One property found")

        browser.$("#pagination li.current").first.text() must equalTo("Displaying 1 to 1 of 1")

        browser.$("#searchbox").write("12345")
        browser.$("#searchsubmit").click()

        browser.$("section h1").first.text() must equalTo("One property found")
        browser.$("section table tbody tr td a", withText("1")).click()

        browser.$("section h1").first.text() must equalTo("Edit property")

        browser.$("#price").write("asd")
        browser.$("input.primary").click()

        browser.$("div.error").size must equalTo(1)
        browser.$("div.error label").first.text() must equalTo("price")

        browser.$("#price").write("2.0")
        browser.$("input.primary").click()

        browser.$("section h1").first.text() must equalTo("One property found")
        browser.$(".alert-message").first.text() must equalTo("Done! Property 1 has been updated")

        browser.$("#searchbox").write("12345")
        browser.$("#searchsubmit").click()

        browser.$("a", withText("1")).click()
        browser.$("input.danger").click()

        browser.$("section h1").first.text() must equalTo("No properties found")
        browser.$(".alert-message").first.text() must equalTo("Done! Property has been deleted")

        browser.$("#searchbox").write("1")
        browser.$("#searchsubmit").click()

        browser.$("section h1").first.text() must equalTo("No properties found")
      }
    }

  }

}
