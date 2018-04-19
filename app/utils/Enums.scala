package utils

import java.util.Date

import models.PriceHistory

class Enums {
  val emptyDouble: Double = Double.NaN
  val emptyLong: Long = 0L
  val emptyDate: Date = new Date(emptyLong)
  val emptyPriceHistory: PriceHistory = PriceHistory(Some(emptyLong), Some(emptyLong), emptyLong, this.emptyDouble)
}

object Enums extends Enums