package macros

import scala.reflect.macros.whitebox

object SimpleCompanionImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    val result = {
      val owner = c.internal.enclosingOwner
      val ownerScope = owner.info.decls
      annottees.map(_.tree).toList match {
        case q"object $name extends ..$parents { ..$body }" :: Nil =>
          val companionObjectSymbol = ownerScope.find(_.name == name).getOrElse {
            c.abort(c.enclosingPosition, s"Name $name not found in scope $owner")
          }
          val traitSymbol = companionObjectSymbol.companion
          println("trait symbol: " + traitSymbol)
          println("object symbol: " + companionObjectSymbol)
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
