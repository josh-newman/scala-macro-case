package macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class RecordCompanion extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro RecordCompanionImpl.impl
}
