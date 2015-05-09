package macros

import scala.reflect.macros.whitebox

private[macros] object RecordImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val (recordTree, recordCompanionTree) = annottees.map(_.tree).toList match {
      case record :: Nil => (record, q"")
      case record :: recordCompanion :: Nil => (record, recordCompanion)
      case _ => c.abort(c.enclosingPosition, s"Expected an annotated `trait` and its companion")
    }

    // TODO: Dedupe with `RecordCompanionImpl`.
    def isRecordField(method: MethodSymbol): Boolean = {
      method.isAbstract && method.isPublic && method.paramLists.isEmpty
    }

    val result = recordTree match {
      // TODO: Add other quasiquotes `trait` fields.
      case q"""
        $mods trait $tpname[..$tparams]
          extends { ..$earlydefns }
          with ..$parents { $self =>
          ..$stats
        }""" =>

        val fieldNames = stats.collect {
          case q"def $tname: $tpt" => tname
        }

        val generatedToString = {
          val fieldsList = fieldNames.map { fieldName =>
            q"""${Constant(fieldName.toString)} + "=" + $fieldName"""
          }
          val listApply = q"_root_.scala.collection.immutable.List.apply"
          q"""
            override def toString(): ${typeOf[String]} = {
              val fieldValues = $listApply[${typeOf[String]}](..$fieldsList).mkString(", ")
              ${Constant(tpname.toString)} + "[" + fieldValues + "]"
            }
          """
        }

        q"""
          $mods trait $tpname[..$tparams]
            extends { ..$earlydefns }
            with ..$parents { $self =>
            ..$stats
            $generatedToString
          }

          $recordCompanionTree
        """
    }

    c.Expr[Any](result)
  }

}
