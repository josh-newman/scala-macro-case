package macros

import scala.reflect.macros.whitebox

object SimpleCompanionImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    val result = {
      annottees.map(_.tree).toList match {
        case q"object $name extends ..$parents { ..$body }" :: Nil =>
          q"""
            object $name extends ..$parents {
              ..$body
              override def toString: ${typeOf[String]} = {
                s"custom toString for " + ${Constant(name.decodedName.toString)}
              }
            }
          """
      }
    }
    c.Expr[Any](result)
  }

}
