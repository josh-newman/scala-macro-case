package macros

import scala.reflect.macros.whitebox

private[macros] object RecordImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val owner = c.internal.enclosingOwner
    val ownerScope = owner.info.decls

    val (recordTree, recordCompanionTree) = annottees.map(_.tree).toList match {
      case record :: Nil => (record, q"")
      case record :: recordCompanion :: Nil => (record, recordCompanion)
      case _ => c.abort(c.enclosingPosition, s"Expected an annotated `trait` and its companion")
    }

    val recordSymbol = recordTree match {
      case q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$stats }" =>
        ownerScope.find(_.name == tpname).getOrElse {
          c.abort(c.enclosingPosition, s"Trait $tpname not found in scope $owner")
        }
    }

    // TODO: Dedupe with `RecordCompanionImpl`.
    def isRecordField(method: MethodSymbol): Boolean = {
      method.isAbstract && method.isPublic && method.paramLists.isEmpty
    }

    val fieldMethods = recordSymbol.info.decls.collect {
      case method: MethodSymbol if isRecordField(method) => method
    }.toList

    val result = recordTree match {
      // TODO: Add other quasiquotes `trait` fields.
      case q"""
        $mods trait $tpname[..$tparams]
          extends { ..$earlydefns }
          with ..$parents { $self =>
          ..$stats
        }""" =>

        val generatedToString = {
          val fieldsList = fieldMethods.map { method =>
            q"""${Constant(method.name.toString)} + "=" + ${method.name}"""
          }
          q"""
            override def toString(): ${typeOf[String]} = {
              val fieldValues = ${typeOf[List[String]]}(..$fieldsList).mkString(", ")
              ${Constant(tpname.toString)} + "[" + fieldValues + "]"
            }
          """
        }

        q"""
          $mods trait $tpname[..$tparams]
            extends { ..$earlydefns }
            with ..$parents { $self =>
            ..$stats
          }

          $recordCompanionTree
        """
    }

    c.Expr[Any](result)
  }

}
