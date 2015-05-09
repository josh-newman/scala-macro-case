package examples

import macros.RecordCompanion

trait Complex {
  def real: Double
  def imag: Double

  def gt(other: Complex): Boolean = abs > other.abs
  def abs: Double = Math.sqrt(real * real + imag * imag)
}

@RecordCompanion
object Complex
