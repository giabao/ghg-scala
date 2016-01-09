package model

import model.KineticCoefficientData.Nitrate

import scala.annotation.tailrec

case class R[T](min: T, max: T) {
  def text = s"$min - $max"
}
case class RNorm(range: R[Double], norm: Double)

case class GhgData(info: InfoData, indirect: IndirectData, direct: DirectData) {
  def electricPower: Double = {
    import ElectricData.CalcMethod._, indirect.electric._
    method match {
      case Method1 => info.power * _1.ratio
      case Method2 => _2.power
      case Method3 => _3.power
    }
  }

  lazy val bien1: Bien1Output = {
    val pPool = direct.d.primaryPool

    val s_ov = direct.d.streamIn.s
    val pr_blBod = pPool.prBOD
    val bod_khuBl = info.power * s_ov * pr_blBod

    val x_ov = direct.d.streamIn.tss
    val pr_blSs = pPool.prSS
    val ss_khuBl = info.power * x_ov * pr_blSs

    Bien1Output(
      s_ov,
      pr_blBod,
      bod_khuBl,
      x_ov,
      pr_blSs,
      ss_khuBl
    )
  }

  /** required: direct.d.aerobicPool.isDefined */
  lazy val bien2 = calcBien2(direct.d.aerobicPool.get, bien1)

  private def calcBien2(pool: DirectTable.PoolData, b1: Bien1Output): Bien2Output = {
    val ae = direct.coef.aerobic
    val s = ae.ks_ * (1 + ae.kd_ * pool.srt) / (pool.srt * (ae.y_ * ae.k_ - ae.kd_) - 1)

    val pPool = direct.d.primaryPool
    val q_v = info.power - pPool.q

    val s_v = direct.d.streamIn.s - b1.bod_khuBl / q_v

    val X = pool.srt / pool.hrtDay * ae.y_ * (s_v - s) / (1 + ae.kd_ * pool.srt)

    val ss_v = b1.x_ov - b1.ss_khuBl / q_v
    val vss_v = 0.85 * ss_v

    val X_nbV = vss_v * (1 - 60D/ 73)
    val X_nb = ae.fd * ae.kd_ *  X * pool.srt + X_nbV * pool.srt / pool.hrtDay


    val V = pool.hrtDay * q_v
    val p_ssBod = X * V / pool.srt

    val p_ssManhTeBao = ae.fd * ae.kd_ * X * V
    val p_ssNbVss = X_nbV * V / pool.hrtDay

    val nit = direct.coef.nitrate
    def m_nit(N: Double) = nit.m_ * N / (nit.kn_ + N) * Nitrate.DO / (Nitrate.Kdo + Nitrate.DO) - nit.kd_
    @inline def srtNit(N: Double) = 1 / m_nit(N)
    def x_nit(N: Double) = srtNit(N) / pool.hrtDay * nit.y_ * N / (1 + nit.kd_ * srtNit(N))
    def p_ssNit(N: Double) = x_nit(N) * V / srtNit(N)
    def p_ss(N: Double) =p_ssBod + p_ssNit(N) + p_ssManhTeBao + p_ssNbVss
    def p_ssBio(N: Double) = p_ss(N) - p_ssNbVss //=..
    def calcN(N: Double) = direct.d.streamIn.tkn - direct.d.streamOut.n - .12 * p_ssBio(N) / q_v
    /** Tính ratio N/ TN */
    def calcNRatio(epsilon: Double, maxLoop: Int): Double = {
      @tailrec
      def calc(rmin: Double, rmax: Double, loop: Int): Double = {
        val len = rmax - rmin
        val r = (rmin + rmax) / 2
        val n0 = r * direct.d.streamIn.tkn
        val n = calcN(n0)
        if (loop >= maxLoop || Math.abs(n - n0) < epsilon) {
          r
        } else {
          val (rmin2, rmax2) = if (n > n0) (r, rmax) else (rmin, r)
          calc(rmin2, rmax2, loop + 1)
        }
      }
      calc(0, 1, 0)
    }

    val N = calcNRatio(.001, 30) * direct.d.streamIn.tkn

    val relation = direct.relation.value
    val bod_ox = q_v * (s_v - s) - relation.rCO2Decay * p_ssBio(N)
    val bod_ox_dnt = relation.rBODDnt * N * q_v
    val bod_khuThuc = if (bod_ox < bod_ox_dnt) bod_ox else bod_ox - bod_ox_dnt
    val co2_khu_bod = relation.yCO2 * bod_khuThuc
    val vss_phanhuy = .85 * V * (ae.kd_ * X + nit.kd_ * x_nit(N))
    val co2_phanhuy = relation.yCO2Decay * vss_phanhuy
    val co2_dnt =  relation.yCO2Dnt * N * q_v
    val co2_tieuThuNit = relation.rCO2Nit * N * q_v

    val co2_quaTrinhHieuKhi = co2_khu_bod + co2_phanhuy + co2_dnt - co2_tieuThuNit
    val n2o = q_v * direct.d.streamIn.tkn * Bien2Output.r_n2o
    val co2_n2o = 296 * n2o //fixme hardcode

    Bien2Output(s, q_v, s_v, X, ss_v, vss_v, X_nbV, X_nb, V, p_ssBod, p_ssManhTeBao, p_ssNbVss,
      N, calcN(N), m_nit(N), srtNit(N), x_nit(N), p_ssNit(N), p_ssBio(N), p_ss(N),
      bod_ox, bod_ox_dnt, bod_khuThuc, co2_khu_bod, vss_phanhuy, co2_phanhuy, co2_dnt, co2_tieuThuNit,
      co2_quaTrinhHieuKhi, n2o, co2_n2o)
  }

  lazy val p_ss = if (direct.d.aerobicPool.isEmpty) ??? else bien2.p_ss

  lazy val bien3: Bien3Output = {
    val b1 = bien1
    val b2 = bien2
    val p_vss_dr = b1.ss_khuBl + b2.p_ss

    val q_xa_ae = direct.d.aerobicPool.fold(0D)(p => p.qxRatio * b2.q_v)
    val q_xa_ane = 0 //fixme
    val q_xa = q_xa_ae + q_xa_ane
    Bien3Output(p_vss_dr, q_xa)
  }
}

case class Bien1Output(s_ov: Double,
                       pr_blBod: Double,
                       bod_khuBl: Double,
                       x_ov: Double,
                       pr_blSs: Double,
                       ss_khuBl: Double)

object Bien2Output {
  val r_n2o = .004 //fixme hardcode
}
case class Bien2Output(s: Double,
                       q_v: Double,
                       s_v: Double,
                       X: Double,
                       ss_v: Double,
                       vss_v: Double,
                       X_nbV: Double,
                       X_nb: Double,
                       V: Double,
                       p_ssBod: Double,
                       p_ssManhTeBao: Double,
                       p_ssNbVss: Double,
                       N: Double,
                       Ncalc: Double,
                       m_nit: Double,
                       srtNit: Double,
                       x_nit: Double,
                       p_ssNit: Double,
                       p_ssBio: Double,
                       p_ss: Double,
                       bod_ox: Double,
                       bod_ox_dnt: Double,
                       bod_khuThuc: Double,
                       co2_khu_bod: Double,
                       vss_phanhuy: Double,
                       co2_phanhuy: Double,
                       co2_dnt: Double,
                       co2_tieuThuNit: Double,
                       co2_quaTrinhHieuKhi: Double,
                       n2o: Double,
                       co2_n2o: Double
                      )

case class Bien3Output(p_vss_dr: Double,
                       q_xa: Double)
