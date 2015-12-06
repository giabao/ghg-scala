package model

import model.ElectricData.CountryPowerStruct
import org.widok.moment.Date

object ElectricData {
  object CalcMethod extends Enumeration {
    type CalcMethod = Value

    val Method1 = Value("Từ hệ số mặc đinh")
    val Method2 = Value("Từ dữ liệu tiêu thụ thực tế")
    val Method3 = Value("Từ các thiết bị trong hệ thống")
  }

  case class D1(ratio: Double, ratioRange: R[Double] = R(0.1, 0.45))

  object D2 {
    case class Row(from: Date, to: Date, kwh: Double) {
      def days = to.diff(from, "days", asFloat = true)
      def average = kwh / days
    }
  }
  /** dates.length must == kwh.length + 1 */
  case class D2(rows: Seq[D2.Row]) {
    def power = rows.foldLeft((0D, 0D)) {
      case ((kwh, days), r) => (kwh + r.kwh, days + r.days)
    } match {
      case (kwh, days) => kwh / days
    }
  }

  object D3 {
    case class Row(device: String, kw: Double, quantity: Int, workHoursPerDay: Int) {
      def operateMode = if (workHoursPerDay < 24) "Gián đoạn" else "Liên tục"
      def kwhPerDay = kw * quantity * workHoursPerDay
    }
  }
  case class D3(rows: Seq[D3.Row], ratio: Double, ratioRange: R[Double] = R(0, 1)) {
    def powerCalc = rows.map(_.kwhPerDay).sum
    def power = ratio * powerCalc
  }

  case class PowerSupply(tpe: String, EFi: Int, EFiRange: R[Int], ref: String)
  /** supplies(x) = PFi of x (a Supply) */
  case class CountryPowerStruct(country: String, ref: String, supplies: Seq[(PowerSupply, Double)]) {
    /** = sum(EFi * PFi) */
    def totalRatio = supplies.map { case (s, pfi) => s.EFi * pfi }.sum
  }
}

case class ElectricData(method: ElectricData.CalcMethod.Value = ElectricData.CalcMethod.Method1,
                        _1: Option[ElectricData.D1] = None,
                        _2: Option[ElectricData.D2] = None,
                        _3: Option[ElectricData.D3] = None,
                        powerStruct: CountryPowerStruct = CountryPowerStruct(
                          "Việt Nam", "Báo cáo thường niên của EVN năm 2013",
                          Seq(
                            ElectricData.PowerSupply("Thủy điện", 10, R(16, 410), "Rasha and Hammad, 2000") -> 48.78,
                            ElectricData.PowerSupply("Hạt nhân", 9, R(9, 30), "Andesta et al., 1998") -> 0,
                            ElectricData.PowerSupply("Than", 877, R(860, 1290), "IPCC,2001") -> 23.07,
                            ElectricData.PowerSupply("Khí tự nhiên", 640, R(460, 1234), "") -> 0,
                            ElectricData.PowerSupply("Sinh học, gió, thủy triều", 11, R(11, 279), "") -> 0.43,
                            ElectricData.PowerSupply("Nhiên liệu khác", 604, R(600, 890), "IPCC,2001") -> 27.72
                          )))