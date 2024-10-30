package toml

import deriving.*, quoted.*, compiletime.*
import toml.Codec.Defaults

private[toml] object Macros {
  
  def derivedMacro[T: Type](using Quotes): Expr[Codec[T]] =
      val ev: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].get

      import quotes.reflect.*

      '{
        new Codec[T] {
          override def apply(value: Value, defaults: Defaults, index: Int): Either[Parse.Error, T] = Left(Nil -> "oh no")
        }
      }
}
