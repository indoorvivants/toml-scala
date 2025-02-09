package toml
package derivation
import shapeless3.deriving.*

trait DefaultParams[P]:
  def defaultParams: Map[String, Any]
object DefaultParams:
  class DefaultParamsGen[P <: Product](f: () => Map[String, Any])
      extends DefaultParams[P]:
    final def defaultParams: Map[String, Any] = f()
  inline given inst[P <: Product](using
      r: K0.ProductGeneric[P]
  ): DefaultParams[P] = DefaultParamsGen { () =>
    macros.defaultParams[P]
  }
