package examples

object Test extends App {
  println(Complex)
  println(Complex(1.0, 2.0))
  println(Complex(1.0, 1.0).abs)
  println(Complex.unapply(Complex(1.0, 2.0)))

  println(Vector3)
  println(Vector3(1.0, 2.0, 3.0))
  println(Vector3(1.0, 2.0, 3.0).length)
  println(Vector3.unapply(Vector3(1.0, 2.0, 3.0)))
  println(Vector3.unit(Vector3(1.0, 2.0, 3.0)))
  println(Vector3.unit(Vector3(1.0, 0.0, 0.0)))
}
