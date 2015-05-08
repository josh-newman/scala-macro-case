package examples

import macros.SimpleCompanion

trait Complex {
  def real: Double
  def imag: Double
}

@SimpleCompanion
object Complex
