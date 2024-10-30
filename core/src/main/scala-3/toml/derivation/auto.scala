package toml.derivation

import toml._

import scala.annotation.implicitNotFound
import scala.deriving.*

object auto {
  given autoDerivationCodec[A: Mirror.Of]: Codec[A] = Codec.derived[A]
}

