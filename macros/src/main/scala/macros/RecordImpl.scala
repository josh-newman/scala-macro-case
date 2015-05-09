package macros

import scala.reflect.macros.whitebox

private[macros] object RecordImpl {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val (recordTree, recordCompanionTreeOption) = annottees.map(_.tree).toList match {
      case record :: Nil => (record, None)
      case record :: recordCompanion :: Nil => (record, Some(recordCompanion))
      case _ => c.abort(c.enclosingPosition, s"Expected an annotated `trait` and its companion")
    }

    val result = recordTree match {
      case q"""
        $recordMods trait $recordTypeName[..$recordTypeParams]
          extends { ..$recordEarlyDefns }
          with ..$recordParents { $recordSelf =>
          ..$recordStatements
        }""" =>

        val fieldNamesAndTypes = recordStatements.collect {
          case q"def $tname: $tpt" => (tname, tpt)
        }

        val recordToStringTree = {
          val fieldsList = fieldNamesAndTypes.map { case (fieldName, _) =>
            q"""${Constant(fieldName.toString)} + "=" + $fieldName"""
          }
          val listApply = q"_root_.scala.collection.immutable.List.apply"
          q"""
            override def toString(): ${typeOf[String]} = {
              val fieldValues = $listApply[${typeOf[String]}](..$fieldsList).mkString(", ")
              ${Constant(recordTypeName.toString)} + "[" + fieldValues + "]"
            }
          """
        }

        val companionGeneratedTrees = {
          val companionApplyTree = {
            val paramsAndImpls = fieldNamesAndTypes.map { case (fieldName, fieldTypeName) =>
              val paramName = c.freshName(fieldName)
              val paramDef = q"val $paramName: $fieldTypeName"
              val implDef = q"override val $fieldName: $fieldTypeName = $paramName"
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

          val companionUnapplyTree = {
            val paramName = c.freshName(TermName("record"))
            val param = q"val $paramName: $recordTypeName"

            val arity = fieldNamesAndTypes.size
            val tupleName = TypeName(s"Tuple$arity")
            val fieldTypes = fieldNamesAndTypes.map(_._2)
            val tupleTree = tq"_root_.scala.$tupleName[..$fieldTypes]"

            val returnTypeTree = tq"_root_.scala.Some[$tupleTree]"

            val tupleArguments = fieldNamesAndTypes.map { case (fieldName, _) =>
              q"$paramName.$fieldName"
            }

            q"""
              def unapply(...${List(List(param))}): $returnTypeTree = {
                _root_.scala.Some(new $tupleTree(..$tupleArguments))
              }
            """
          }

          val companionToStringTree = {
            val fieldNamesString = fieldNamesAndTypes.map(_._1).mkString(", ")
            q"""
              override def toString(): ${typeOf[String]} = {
                ${Constant(recordTypeName.toString)} + "[" + ${Constant(fieldNamesString)} + "]"
              }
            """
          }

          List(
            companionApplyTree,
            companionUnapplyTree,
            companionToStringTree)
        }

        val recordCompanionTree = recordCompanionTreeOption match {
          case Some(q"""
            $companionMods object $companionTypeName
              extends { ..$companionEarlyDefns }
              with ..$companionParents { $companionSelf =>
              ..$companionStatements
            }""") if companionTypeName == recordTypeName.toTermName =>

            q"""
              $companionMods object $companionTypeName
                extends { ..$companionEarlyDefns }
                with ..$companionParents { $companionSelf =>
                ..$companionStatements
                ..$companionGeneratedTrees
              }"""

          case None =>
            q"""
              object ${recordTypeName.toTermName} {
                ..$companionGeneratedTrees
              }"""

          case _ =>
            c.abort(c.enclosingPosition, s"Unrecognized companion object for $recordTypeName")
        }

        q"""
          $recordMods trait $recordTypeName[..$recordTypeParams]
            extends { ..$recordEarlyDefns }
            with ..$recordParents { $recordSelf =>
            ..$recordStatements
            $recordToStringTree
          }

          $recordCompanionTree
        """

      case _ =>
        c.abort(c.enclosingPosition, s"Unrecognized record type")
    }

    c.Expr[Any](result)
  }

}
