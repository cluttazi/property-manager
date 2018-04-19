package dao

import javax.inject.{Inject, Singleton}
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class PropertiesDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends PricesHistoriesComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  class Properties(tag: Tag) extends Table[Property](tag, Property.getClass.getSimpleName.toUpperCase.dropRight(1)) {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def address = column[String]("ADDRESS")

    def postcode = column[String]("POSTCODE")

    def latitude = column[Double]("LATITUDE")

    def longitude = column[Double]("LONGITUDE")

    def surface = column[Option[Double]]("SURFACE")

    def bedrooms = column[Option[Int]]("BEDROOMS")

    def price = column[Double]("PRICE")

    def * = (id.?, address, postcode, latitude, longitude, surface, bedrooms, price) <> (Property.tupled, Property.unapply _)
  }

  private val properties = TableQuery[Properties]
  private val pricesHistories = TableQuery[PricesHistories]

  /** Retrieve a property from the id. */
  def findById(id: Long): Future[Option[Property]] =
    db.run(properties.filter(_.id === id).result.headOption)

  /** Count all properties. */
  def count(): Future[Int] = {
    db.run(properties.map(_.id).length.result)
  }

  /** Count properties with a filter. */
  def count(filter: String): Future[Int] = {
    db.run(properties.filter { property => property.postcode.toLowerCase like filter.toLowerCase }.length.result)
  }

  /** Return a page of (Property) */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Property)]] = {

    val offset = pageSize * page
    val query =
      (for {
        property <- properties
        if property.postcode.toLowerCase like filter.toLowerCase
      } yield (property))
        .drop(offset)
        .take(pageSize)

    for {
      totalRows <- count(filter)
      list = query.result.map { rows =>
        rows.collect { case (property) => {
          (property)
        }
        }
      }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }

  /** Insert a property. */
  def insert(property: Property): Future[Unit] =
    db.run(properties += property).map(_ => ())

  /** Update a property. */
  def update(id: Long, property: Property): Future[Unit] = {
    val propertyToUpdate: Property = property.copy(Some(id))

    db.run(properties.filter(_.id === id).update(propertyToUpdate)).map(_ => ())
  }

  /** Delete a property. */
  def delete(id: Long): Future[Unit] =
    db.run(properties.filter(_.id === id).delete).map(_ => ())

}