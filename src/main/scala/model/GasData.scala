package model

import monocle.macros.Lenses
import org.widok.moment.{Moment, Date}

object GasData {
  @Lenses case class PowerRow(from: Date, to: Date, m3: Double) {
    def days = to.diff(from, "days", asFloat = true) + 1
    def average = m3 / days
  }
  object PowerRow {
    def apply(from: String, to: String, m3: Double): PowerRow =
      PowerRow(Moment(from, "DD/MM/YYYY"), Moment(to, "DD/MM/YY"), m3)

    def apply(): PowerRow = {
      val today = Moment().startOf("d")
      PowerRow(today.subtract(1, "d"), today, 0)
    }
  }

//  object GasRatioTable {
//    case class Row(co2: Double, ch4: Double, n2o: Double,
//                   tpe: String = "",
//                   active: Boolean = false)
//    case class Group(step: String, rows: Seq[Row])
//  }
//  case class GasRatioTable (ref: String, rows: Seq[GasRatioTable.Group]) {
//    def efCO2 = rows.flatMap(_.rows).filter(_.active).map(_.co2).sum
//    def efCH4 = rows.flatMap(_.rows).filter(_.active).map(_.ch4).sum
//    def efN2O = rows.flatMap(_.rows).filter(_.active).map(_.n2o).sum
//  }

  @Lenses case class GasRatio(ref: String, co2: Double, ch4: Double, n2o: Double)
  @Lenses case class GWP(ref: String, co2: Double, ch4: Double, n2o: Double)
//  object GWP {
//    case class Row(co2: Double, ch4: Double, n2o: Double)
//  }
//  /** @param rows Seq[số năm -> Row] */
//  case class GWP(ref: String, selectedIdx: Int, rows: Seq[(Int, GWP.Row)]) {
//    def value = rows(selectedIdx)._2
//  }
}

case class GasData(powers: Seq[GasData.PowerRow],
                   gas: GasData.GasRatio,
                   gwp: GasData.GWP) {
  def power = powers.foldLeft((0D, 0D)) {
    case ((m3, days), r) => (m3 + r.m3, days + r.days)
  } match {
    case (m3, days) => m3 / days
  }

  def ghg = power * (gas.co2 * gwp.co2 + gas.ch4 * gwp.ch4 + gas.n2o * gwp.n2o) / 1000 //kg
}