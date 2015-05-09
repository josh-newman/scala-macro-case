package macros

import scala.reflect.macros.whitebox

object SimpleCompanionImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val owner = c.internal.enclosingOwner
    val ownerScope = owner.info.decls

    val recordCompanionTree = annottees.map(_.tree).toList match {
      case head :: Nil => head
      case _ => c.abort(c.enclosingPosition, "Expected a single annotated tree")
    }

    val recordCompanionSymbol = recordCompanionTree match {
      case q"object $name" =>
        ownerScope.find(_.name == name).getOrElse {
          c.abort(c.enclosingPosition, s"Object $name not found in scope $owner")
        }
    }

    val recordSymbol = recordCompanionSymbol.companion

    def isRecordField(method: MethodSymbol): Boolean = {
      method.isAbstract && method.isPublic && method.paramLists.isEmpty
    }

    val fieldMethods = recordSymbol.info.decls.collect {
      case method: MethodSymbol if isRecordField(method) => method
    }.toList

    val fieldNames = fieldMethods.map(_.name)
    val fieldTypes = fieldMethods.map(_.returnType)

    println("field names: " + fieldNames)
    println("field types: " + fieldTypes)

    val result = recordCompanionTree match {
      case q"object $name extends ..$parents { ..$body }" =>
        val generatedToString = {
          val fieldNamesString = fieldNames.mkString(", ")
          q"""
            override def toString: ${typeOf[String]} = {
              ${Constant(name.decodedName.toString)} + "[" + ${Constant(fieldNamesString)} + "]"
            }
          """
        }

        q"""
          object $name extends ..$parents {
            ..$body
            $generatedToString
          }
        """
    }

    c.Expr[Any](result)
  }

}
