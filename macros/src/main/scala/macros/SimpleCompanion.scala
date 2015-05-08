package macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class SimpleCompanion extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro SimpleCompanionImpl.impl
}
