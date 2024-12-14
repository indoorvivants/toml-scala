package toml

import toml.derivation.DefaultParams
import shapeless3.deriving.K0.CoproductGeneric
trait TomlVersionSpecific:
    self:Toml.type =>
        final def parseAs[T](input: Value.Tbl | String)(
            using Codec[T], DefaultParams[T]
        ):Either[Parse.Error, T] = parseAs(input, Set.empty)

        final def parseAs[T](
            input: Value.Tbl | String,
            extensions: Set[Extension]
        )(using  codec: Codec[T], D: DefaultParams[T]
        ):Either[Parse.Error, T] = input match
            case toml: String =>
                parse(toml, extensions).flatMap(codec(_,D.defaultParams,0))
            case table: Value.Tbl => codec(
            table,
            D.defaultParams,
            0
        )
        final class CodecHelperValue[A]:
            def apply(value: Value)(using codec: Codec[A]): Either[Parse.Error, A] =
            codec(value, Map(), 0)

            def apply(toml: String, extensions: Set[Extension] = Set())(using
                codec: Codec[A]
            ): Either[Parse.Error, A] =
            parse(toml, extensions).right.flatMap(codec(_, Map(), 0))
        end CodecHelperValue

        final def parseAsValue[T]: CodecHelperValue[T] = new CodecHelperValue[T]


