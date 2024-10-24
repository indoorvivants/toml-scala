package toml

import scala.annotation.implicitNotFound

@implicitNotFound("Codec[${A}] implicit not defined in scope")
trait Codec[A] {
  def apply(
    value:    Value,
    defaults: Codec.Defaults,
    index:    Int
  ): Either[Parse.Error, A]
}

object Codec {
  type Defaults = Map[String, Any]
  type Index    = Int

  def apply[T](
    f: (Value, Defaults, Index) => Either[Parse.Error, T]
  ): Codec[T] = new Codec[T] {
    override def apply(
      value: Value, defaults: Defaults, index: Index
    ): Either[Parse.Error, T] = f(value, defaults, index)
  }


  implicit val stringCodec: Codec[String] = Codec {
    case (Value.Str(value), _, _) => Right(value)
    case (value,            _, _) =>
      Left((List.empty, s"String expected, $value provided"))
  }

  implicit val longCodec: Codec[Long] = Codec {
    case (Value.Num(value), _, _) => Right(value)
    case (value           , _, _) =>
      Left((List.empty, s"Long expected, $value provided"))
  }

  implicit val intCodec: Codec[Int] = Codec {
    case (Value.Num(value), _, _) => Right(value.toInt)
    case (value           , _, _) =>
      Left((List.empty, s"Int expected, $value provided"))
  }

  implicit val doubleCodec: Codec[Double] = Codec {
    case (Value.Real(value), _, _) => Right(value)
    case (value            , _, _) =>
      Left((List.empty, s"Double expected, $value provided"))
  }

  implicit val boolCodec: Codec[Boolean] = Codec {
    case (Value.Bool(value), _, _) => Right(value)
    case (value            , _, _) =>
      Left((List.empty, s"Bool expected, $value provided"))
  }

  implicit def listCodec[T](implicit codec: Codec[T]): Codec[List[T]] = Codec {
    case (Value.Arr(elems), _, _) =>
      elems.zipWithIndex.foldLeft(Right(List.empty): Either[Parse.Error, List[T]]) {
        case (Right(acc), (cur, idx)) =>
          codec(cur, Map.empty, 0)
            .left.map { case (a, m) => (s"#${idx + 1}" +: a, m) }
            .right.map(acc :+ _)

        case (acc, _) => acc
      }

    case (value, _, _) => Left((List.empty, s"List expected, $value provided"))
  }

  implicit def tableCodec[T](implicit codec: Codec[T]): Codec[Map[String, T]] =
    Codec {
      case (Value.Tbl(value), _, _) =>
        value.foldLeft(Right(Map.empty): Either[Parse.Error, Map[String, T]]) {
          case (Left(l), _) => Left(l)
          case (Right(r), (k, v)) =>
            codec(v, Map.empty, 0) match {
              case Left((a, m)) => Left((k +: a, m))
              case Right(v2)    => Right(r + (k -> v2))
            }
        }

      case (value, _, _) => Left((List.empty, s"Table expected, $value provided"))
    }
}
