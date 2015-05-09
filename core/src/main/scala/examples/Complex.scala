package examples

import macros.SimpleCompanion

trait Complex {
  def real: Double
  def imag: Double

  def gt(other: Complex): Boolean = abs > other.abs
  def abs: Double = Math.sqrt(real * real + imag * imag)
}

@SimpleCompanion
object Complex
