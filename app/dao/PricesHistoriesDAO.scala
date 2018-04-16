package dao

import java.util.Date

import javax.inject.{Inject, Singleton}
import models.{PriceHistory, Property}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait PricesHistoriesComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class PricesHistories(tag: Tag) extends Table[PriceHistory](tag, "PRICEHISTORY") {
    implicit val dateColumnType = MappedColumnType.base[Date, Long](d => d.getTime, d => new Date(d))

    def property = column[Option[Long]]("PROPERTY", O.PrimaryKey)

    def date = column[Date]("DATE", O.PrimaryKey)

    def price = column[Double]("PRICE")

    def * = (property, date, price) <> (PriceHistory.tupled, PriceHistory.unapply _)
  }

}

@Singleton()
class PricesHistoriesDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends PricesHistoriesComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val pricesHistories = TableQuery[PricesHistories]

  //  /** Construct the Map[Date, Double] needed to fill a select options set */
  //  def options(): Future[Seq[(Date, Double)]] = {
  //    val query = for {
  //      priceHistory <- pricesHistories
  //    } yield (priceHistory.date, priceHistory.price) //.sortBy(/*date*/ _)
  //
  //    db.run(query.result).map(rows => rows.map { case (date, price) => (date, price) })
  //  }

  /** Insert a new price */
  def insert(priceHistory: PriceHistory): Future[Unit] =
    db.run(pricesHistories += priceHistory).map(_ => ())

  /** Retrieve a list of prices from a property id. */
  def findByProperty(property: Long): Future[Seq[(Date, Double)]] =
    db.run(pricesHistories.filter(_.property === property).map(p => (p.date, p.price)).result)

}