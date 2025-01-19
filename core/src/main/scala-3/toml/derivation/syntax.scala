package toml
package derivation
import shapeless3.deriving.*

private[toml] trait DerivedSyntax:
  self: Codec.type =>
  inline def derived[P](using
      inst: K0.ProductInstances[Codec, P],
      labelling: Labelling[P],
      inline d: DefaultParams[P]
  ): Codec[P] = DerivedProductCodec.codecGen[P]
