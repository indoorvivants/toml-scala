package toml
package derivation
import toml.Codec.Defaults

object auto:
  inline implicit def derivedProductCodec[P](using
      inline codec: DerivedProductCodec[P]
  ): Codec[P] = codec

  implicit def op[A](implicit c: Codec[A]): Codec[Option[A]] = new Codec:
    def apply(
        value: Value,
        defaults: Defaults,
        index: Int
    ): Either[Parse.Error, Option[A]] =
      c.apply(value, defaults, index).map(Some(_))
    override def optional: Boolean = true
end auto
