package toml

import scala.deriving.Mirror

private[toml] trait CodecVersionSpecific {
  inline def derived[A: Mirror.Of] = ${Macros.derivedMacro[A]}
}
