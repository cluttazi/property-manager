package models

import java.util.Date

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

case class PriceHistory(property: Option[Long], date: Date, price: Double)

case class Property(id: Option[Long] = None, address: String, postcode: String, latitude: Double, longitude: Double,
                    surface: Option[Double] = None, bedrooms: Option[Int] = None, price: Double)