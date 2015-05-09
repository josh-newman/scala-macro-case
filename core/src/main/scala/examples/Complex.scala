package examples

import macros.Record
import macros.RecordCompanion

@Record
trait Complex {
  def real: Double
  def imag: Double

  def gt(other: Complex): Boolean = abs > other.abs
  def abs: Double = Math.sqrt(real * real + imag * imag)
}

@Record
trait Vector3 {
  def x: Double
  def y: Double
  def z: Double

  def length: Double = Math.sqrt(x * x + y * y + z * z)
}

object Vector3 {

  def unit(other: Vector3): Vector3 = {
    val otherLength = other.length
    Vector3(other.x / otherLength, other.y / otherLength, other.z / otherLength)
  }

}