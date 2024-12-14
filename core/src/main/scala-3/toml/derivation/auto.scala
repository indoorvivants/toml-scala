package toml
package derivation
import shapeless3.deriving.*
import toml.Codec.Defaults

object auto:
  inline implicit def derivedProductCodec[P](using
    inline codec: DerivedProductCodec[P],
  ): Codec[P] = codec

  inline implicit def derivedProductOptionCodec[P](using
    inline codec: DerivedProductCodec[Option[P]],
  ): Codec[Option[P]] = codec


