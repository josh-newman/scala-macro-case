package macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class Record extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro RecordImpl.impl
}
