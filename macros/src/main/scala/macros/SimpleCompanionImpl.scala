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
    val recordTypeName = recordSymbol.name.toTypeName

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

        val generatedApply = {
          val paramsAndImpls = fieldMethods.map { method =>
            val paramName = c.freshName(method.name)
            val paramDef = q"val $paramName: ${method.returnType}"
            val implDef = q"override val ${method.name}: ${method.returnType} = $paramName"
            (paramDef, implDef)
          }

          val paramss = paramsAndImpls.map(_._1) :: Nil
          val impls = paramsAndImpls.map(_._2)

          q"""
            def apply(...$paramss): $recordTypeName = new $recordTypeName {
              ..$impls
            }
          """
        }

        q"""
          object $name extends ..$parents {
            ..$body
            $generatedApply
            $generatedToString
          }
        """
    }

    c.Expr[Any](result)
  }

}
