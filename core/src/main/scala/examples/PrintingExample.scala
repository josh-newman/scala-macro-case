package examples

object PrintingExample extends App {
  println(
    s"""`Complex` examples:
    |Complex: ${Complex}
    |Complex(1.0, 2.0): ${Complex(1.0, 2.0)}
    |Complex(1.0, 1.0).abs: ${Complex(1.0, 1.0).abs}
    |Complex.unapply(Complex(1.0, 2.0)): ${Complex.unapply(Complex(1.0, 2.0))}
    |
    |`Vector3` examples:
    |Vector3: ${Vector3}
    |Vector3(1.0, 2.0, 3.0): ${Vector3(1.0, 2.0, 3.0)}
    |Vector3(1.0, 2.0, 3.0).length: ${Vector3(1.0, 2.0, 3.0).length}
    |Vector3.unapply(Vector3(1.0, 2.0, 3.0)): ${Vector3.unapply(Vector3(1.0, 2.0, 3.0))}
    |Vector3.unit(Vector3(1.0, 2.0, 3.0)): ${Vector3.unit(Vector3(1.0, 2.0, 3.0))}
    |Vector3.unit(Vector3(1.0, 0.0, 0.0)): ${Vector3.unit(Vector3(1.0, 0.0, 0.0))}""".stripMargin)
}
