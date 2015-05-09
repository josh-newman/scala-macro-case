package examples

object Test extends App {
  println(Complex)
  println(Complex(1.0, 2.0))
  println(Complex(1.0, 1.0).abs)
  println(Complex.unapply(Complex(1.0, 2.0)))
}
