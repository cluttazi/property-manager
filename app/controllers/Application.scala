package controllers

import dao.{PricesHistoriesDAO, PropertiesDAO}
import javax.inject.Inject
import models.{PriceHistory, Property}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText, optional, _}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.Enums
import views.html

import scala.concurrent.{ExecutionContext, Future}

/** Manage a database of properties. */
class Application @Inject()(
                             pricesHistoriesDAO: PricesHistoriesDAO,
                             propertiesDAO: PropertiesDAO,
                             controllerComponents: ControllerComponents
                           )(implicit executionContext: ExecutionContext) extends AbstractController(controllerComponents) with I18nSupport {

  /** This result directly redirect to the application home. */
  val Home = Redirect(routes.Application.list(0, 2, ""))

  import play.api.data.format.Formats._

  /** Describe the property form (used in both edit and create screens). */
  val propertyForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "address" -> nonEmptyText,
      "postcode" -> nonEmptyText,
      "latitude" -> of(doubleFormat),
      "longitude" -> of(doubleFormat),
      "surface" -> optional(of(doubleFormat)),
      "bedrooms" -> optional(number),
      "price" -> of(doubleFormat)
    )(Property.apply)(Property.unapply)
  )

  // -- Actions

  /** Handle default path requests, redirect to property list */
  def index = Action {
    Home
  }

  /**
    * Display the paginated list of properties.
    *
    * @param page    Current page number (starts from 0)
    * @param orderBy Column to be sorted
    * @param filter  Filter applied on property names
    */
  def list(page: Int, orderBy: Int, filter: String) = Action.async { implicit rs =>
    val properties = propertiesDAO.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%"))
    properties.map(cs => Ok(html.list(cs, orderBy, filter)))
  }

  /**
    * Display the paginated list of prices.
    *
    * @param page    Current page number (starts from 0)
    * @param orderBy Column to be sorted
    * @param filter  Filter applied on property names
    */
  def listPrices(page: Int, orderBy: Int, filter: String) = Action.async { implicit rs =>
    val properties = pricesHistoriesDAO.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%"))
    properties.map(cs => Ok(html.listPrices(cs, orderBy, filter)))
  }

  /**
    * Display the 'edit form' of a existing Property.
    *
    * @param id Id of the property to edit
    */
  def edit(id: Long) = Action.async { implicit rs =>
    val futureProperty: Future[Option[Property]] = propertiesDAO.findById(id)
    futureProperty.map(i => Ok(html.editForm(id, propertyForm.fill(i.get))))
  }

  /**
    * Handle the 'edit form' submission
    *
    * @param id Id of the property to edit
    */
  def update(id: Long) = Action.async { implicit rs =>
    propertyForm.bindFromRequest.fold(
      formWithErrors => scala.concurrent.Future {
        BadRequest(html.editForm(id, formWithErrors))
      },
      property => {
        for {
          _ <- {
            propertiesDAO.update(id, property)
            pricesHistoriesDAO.insert(PriceHistory(Some(Enums.emptyLong), Some(id), System.currentTimeMillis, property.price))
          }
        } yield
          Home.flashing("success" -> s"Property ${id} has been updated")
      }
    )
  }

  /** Display the 'new property form'. */
  def create = Action.async { implicit rs =>
    scala.concurrent.Future {
      Ok(html.createForm(propertyForm))
    }
  }

  /** Handle the 'new property form' submission. */
  def save = Action.async { implicit rs =>
    propertyForm.bindFromRequest.fold(
      formWithErrors => scala.concurrent.Future {
        BadRequest(html.createForm(formWithErrors))
      },
      property => {
        for {
          _ <- {
            propertiesDAO.insert(property)
          }
        } yield Home.flashing("success" -> s"Property ${property} has been created")
      }
    )
  }

  /** Handle computer deletion. */
  def delete(id: Long) = Action.async { implicit rs =>
    for {
      _ <- {
        propertiesDAO.delete(id)
        pricesHistoriesDAO.deleteByProperty(id)
      }
    } yield Home.flashing("success" -> "Property has been deleted")
  }
}
