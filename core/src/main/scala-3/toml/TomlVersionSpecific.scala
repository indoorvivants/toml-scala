package toml

import toml.derivation.DefaultParams
import shapeless3.deriving.K0.ProductGeneric
trait TomlVersionSpecific:
    self:Toml.type =>
        class CodecHelperGeneric[T]:
            def apply(table: Value.Tbl)(
                using codec: Codec[T], D: DefaultParams[T]
            ): Either[Parse.Error, T] =
                codec(table, D.defaultParams, 0)

            def apply(
                toml: String,
                extensions: Set[Extension]
            )(using codec: Codec[T], D:DefaultParams[T]): Either[Parse.Error, T] =
                parse(toml, extensions).flatMap(codec(_,D.defaultParams,0))

            def apply(
                toml: String
            )(using codec: Codec[T], D:DefaultParams[T]): Either[Parse.Error, T] =
                apply(toml, Set.empty)
        end CodecHelperGeneric

        final class CodecHelperValue[A]:
            def apply(value: Value)(using codec: Codec[A]): Either[Parse.Error, A] =
            codec(value, Map(), 0)

            def apply(toml: String, extensions: Set[Extension] = Set())(using
                codec: Codec[A]
            ): Either[Parse.Error, A] =
            parse(toml, extensions).right.flatMap(codec(_, Map(), 0))
        end CodecHelperValue

        final def parseAs[T]: CodecHelperGeneric[T] = new CodecHelperGeneric[T]

        final def parseAsValue[T]: CodecHelperValue[T] = new CodecHelperValue[T]


