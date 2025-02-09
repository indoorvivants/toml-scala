package toml.derivation

import shapeless.*
import shapeless.labelled.*
import toml.*

object auto extends LowPriorityCodecs with PlatformCodecs {
  implicit val hnilFromNode: Codec[HNil] =
    Codec[HNil] {
      case (Value.Tbl(pairs), defaults, _) if pairs.nonEmpty =>
        Left((List(pairs.keySet.head), "Unknown field"))
      case (Value.Arr(elems), defaults, _) if elems.nonEmpty =>
        Left((List(), s"Too many elements; remove ${elems.head}"))
      case _ => Right(HNil)
    }

  implicit def genericCodec[A, D <: HList, R <: HList](implicit
      generic: LabelledGeneric.Aux[A, R],
      defaults: Default.AsRecord.Aux[A, D],
      defaultMapper: util.RecordToMap[D],
      codec: Codec[R]
  ): Codec[A] = {
    val d = defaultMapper(defaults())
    Codec((v, _, _) => codec(v, d, 0).right.map(generic.from))
  }
}

/** Adapted from: https://stackoverflow.com/a/31641779
  */
trait LowPriorityCodecs {
  implicit def hconsFromNodeOpt[K <: Symbol, V, T <: HList](implicit
      witness: Witness.Aux[K],
      fromV: Lazy[Codec[V]],
      fromT: Lazy[Codec[T]]
  ): Codec[FieldType[K, Option[V]] :: T] = {
    import witness.value.{name => witnessName}

    def f(
        head: Option[Value],
        tail: Value,
        mapError: Parse.Error => Parse.Error,
        default: Option[V],
        defaults: Codec.Defaults,
        index: Codec.Index
    ) =
      fromT
        .value(tail, defaults, index + 1)
        .right
        .flatMap(t =>
          head match {
            case None => Right(field[K](default) :: t)
            case Some(v) =>
              for {
                k <- fromV.value(v, defaults, index).left.map(mapError).right
              } yield field[K](Some(k)) :: t
          }
        )

    def resolve(defaults: Map[String, Any], key: String): Option[V] =
      defaults.get(key).asInstanceOf[Option[Option[V]]].flatten

    Codec {
      case (Value.Tbl(pairs), defaults, _) =>
        f(
          pairs.get(witnessName),
          Value.Tbl(pairs - witnessName),
          { case (a, m) => (witnessName +: a, m) },
          resolve(defaults, witnessName),
          defaults,
          0
        )

      case (Value.Arr(values), defaults, index) =>
        f(
          values.headOption,
          Value.Arr(values.drop(1)),
          { case (a, m) => (s"#${index + 1}" +: a, m) },
          resolve(defaults, witnessName),
          defaults,
          index
        )

      case (value, _, _) =>
        Left((List(), s"Table or Array expected, $value provided"))
    }
  }

  implicit def hconsFromNode[K <: Symbol, V, T <: HList](implicit
      witness: Witness.Aux[K],
      fromV: Lazy[Codec[V]],
      fromT: Lazy[Codec[T]]
  ): Codec[FieldType[K, V] :: T] = {
    import witness.value.{name => witnessName}

    def f(
        head: Value,
        tail: Value,
        mapError: (Parse.Error, Codec.Index) => Parse.Error,
        defaults: Codec.Defaults,
        index: Codec.Index
    ) =
      for {
        h <- fromV
          .value(head, defaults, index)
          .left
          .map(mapError(_, index))
          .right
        t <- fromT.value(tail, defaults, index + 1).right
      } yield field[K](h) :: t

    Codec {
      case (Value.Tbl(pairs), defaults, _) if pairs.contains(witnessName) =>
        f(
          pairs(witnessName),
          Value.Tbl(pairs - witnessName),
          { case ((a, m), _) => (witnessName +: a, m) },
          defaults,
          0
        )

      case (Value.Arr(head +: tail), defaults, index) =>
        f(
          head,
          Value.Arr(tail),
          { case ((a, m), index) => (s"#${index + 1}" +: a, m) },
          defaults,
          index
        )

      case (value, defaults, index)
          if defaults.contains(witnessName) && (
            value.isInstanceOf[Value.Tbl] || value.isInstanceOf[Value.Arr]
          ) =>
        val h = defaults(witnessName).asInstanceOf[V]
        fromT.value(value, defaults, index).right.map(t => field[K](h) :: t)

      case (_, _, _) =>
        Left((List(), s"Cannot resolve `$witnessName`"))
    }
  }
}
