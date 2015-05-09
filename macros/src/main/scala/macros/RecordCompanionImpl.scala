package macros

import scala.reflect.macros.whitebox

private[macros] object RecordCompanionImpl {

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

    val result = recordCompanionTree match {
      case q"object $name extends ..$parents { ..$body }" =>
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

        val generatedUnapply = {
          val paramName = c.freshName(TermName("record"))
          val param = q"val $paramName: $recordTypeName"

          val arity = fieldMethods.size
          val tupleName = TypeName(s"Tuple$arity")
          val fieldTypes = fieldMethods.map(_.returnType)
          val tupleTree = tq"_root_.scala.$tupleName[..$fieldTypes]"

          val returnTypeTree = tq"_root_.scala.Some[$tupleTree]"

          val tupleArguments = fieldMethods.map { method =>
            q"$paramName.${method.name}"
          }

          q"""
            def unapply(...${List(List(param))}): $returnTypeTree = {
              _root_.scala.Some(new $tupleTree(..$tupleArguments))
            }
          """
        }

        val generatedToString = {
          val fieldNamesString = fieldMethods.map(_.name).mkString(", ")
          q"""
            override def toString: ${typeOf[String]} = {
              ${Constant(name.decodedName.toString)} + "[" + ${Constant(fieldNamesString)} + "]"
            }
          """
        }

        q"""
          object $name extends ..$parents {
            ..$body
            $generatedApply
            $generatedUnapply
            $generatedToString
          }
        """
    }

    c.Expr[Any](result)
  }

}
