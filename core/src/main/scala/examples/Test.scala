package examples

import macros.SimpleCompanion

@SimpleCompanion
object Test extends App {
  println(this.hello)
}
