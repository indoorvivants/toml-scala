package toml.derivation.auto

import toml._

import scala.annotation.implicitNotFound
import scala.deriving.*

inline given autoDerivationCodec[A: Mirror.Of]: Codec[A] = Codec.derived[A]
