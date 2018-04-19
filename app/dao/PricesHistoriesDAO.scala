package dao

import javax.inject.{Inject, Singleton}
import models.{Page, PriceHistory}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait PricesHistoriesComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class PricesHistories(tag: Tag) extends Table[PriceHistory](tag, PriceHistory.getClass.getSimpleName.toUpperCase.dropRight(1)) {

    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)

    def property = column[Option[Long]]("PROPERTY")

    def timestamp = column[Long]("TIMESTAMP")

    def price = column[Double]("PRICE")

    def * = (id, property, timestamp, price) <> (PriceHistory.tupled, PriceHistory.unapply _)
  }

}

@Singleton()
class PricesHistoriesDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends PricesHistoriesComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val pricesHistories = TableQuery[PricesHistories]

  /** Insert a new price */
  def insert(priceHistory: PriceHistory): Future[Unit] =
    db.run(pricesHistories += priceHistory).map(_ => ())

  /** Retrieve a list of prices from a property id. */
  def findByProperty(property: Long): Future[Seq[PriceHistory]] =
    db.run(pricesHistories.filter(_.property === property).result)

  /** Delete a priceHistory. */
  def deleteByProperty(id: Long): Future[Unit] =
    db.run(pricesHistories.filter(_.property === id).delete).map(_ => ())

  /** Count all properties. */
  def count(): Future[Int] = {
    db.run(pricesHistories.map(_.property).length.result)
  }

  /** Return a page of (Price) */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(PriceHistory)]] = {

    val offset = pageSize * page
    val query =
      (for {
        price <- pricesHistories
      } yield (price))
        .drop(offset)
        .take(pageSize)

    for {
      totalRows <- count()
      list = query.result.map { rows =>
        rows.collect { case (price) => {
          (price)
        }
        }
      }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }

}