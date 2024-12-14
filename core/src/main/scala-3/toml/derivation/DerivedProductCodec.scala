package toml
package derivation
import toml.Codec.Defaults
import toml.Codec.Index
import shapeless3.deriving.*
import shapeless3.deriving.K1.ProductGeneric

trait DerivedProductCodec[P] extends Codec[P]
object DerivedProductCodec:
  given [P <: Product](using
    labelled: Labelling[P],
    inst: K0.ProductInstances[Codec, P],
    d: DefaultParams[P]
  ): DerivedProductCodec[Option[P]] with
    type Result[A] = Either[Parse.Error, Option[A]]
    override def optional: Boolean = true
    def apply(value: Value, defaults : Defaults, ___ : Int): Either[Parse.Error, Option[P]] =
      val labels = labelled.elemLabels.iterator.zipWithIndex

      val decodeField =
        [t] => (codec: Codec[t]) =>
          value match
            case Value.Tbl(map) =>
              val (witnessName, _) = labels.next()
              map.get(witnessName) match
                case Some(value) =>
                  codec(value, d.defaultParams, 0)
                    .map(Some(_))
                    .left.map((a,m) => (witnessName +: a, m))
                case None =>
                  Right(
                    d.defaultParams.get(witnessName)
                      .map(_.asInstanceOf[t])
                  )
            case Value.Arr(values) =>
              Left((Nil, "Not Implemented"))
      val combineFields: Ap[[a] =>> Result[a]] =
        [a, b] =>
          (ff: Result[a => b], fa: Result[a]) =>
            (fa, ff) match
              case (Left(e),Right(_)) => Left(e)
              case (_,Left(e)) => Left(e)
              case (Right(Some(a)),Right(Some(f))) => Right(Some(f(a)))
              case (Right(_),Right(_)) => Right(None)

      inst.constructA[Result](decodeField)(
        pure = [a] => (x: a) => Right(Some(x)),
        map = [a, b] => (fa: Result[a], f: a => b) => fa.map:
          case Some(a) => Some(f(a))
          case None => None,
        combineFields
      )


  given [P <: Product](using
    labelled: Labelling[P],
    inst: K0.ProductInstances[Codec, P],
    d: DefaultParams[P],
  ): DerivedProductCodec[P] with
    override def apply(value: Value, __ : Defaults, ___ :Index): Either[Parse.Error, P] =
      val labels = labelled.elemLabels.iterator.zipWithIndex
      val labelsSet = labelled.elemLabels.toSet

      def validateNoExtraField(map: Map[String, Value]) =
        map.keySet.diff(labelsSet).headOption match
          case None => Right(())
          case Some(unknownField) =>
            Left((List(unknownField), "Unknown field"))

      val decodeField =
        [t] => (codec: Codec[t]) =>
          value match
              case Value.Tbl(map) =>
                for
                  _ <- validateNoExtraField(map)
                  (witnessName, _) = labels.next()
                  result <- map.get(witnessName) match
                    case Some(value) => codec.apply(value, d.defaultParams, 0)
                      .left.map((a,m) => (witnessName +: a, m))
                    case None =>
                      d.defaultParams.get(witnessName) match
                        case None if codec.optional => Right(None.asInstanceOf[t])
                        case None => Left((Nil,s"Cannot resolve `$witnessName`"))
                        case Some(value) => Right(value.asInstanceOf[t])
                yield result
              case Value.Arr(values) if values.nonEmpty =>
                labels.nextOption() match
                  case Some((_,idx)) if idx < values.length =>
                    codec.apply(values(idx), d.defaultParams, idx)
                      .left.map((a,m) => (s"#${idx + 1}" +: a, m))
                  case Some((witnessName,idx)) if d.defaultParams.contains(witnessName) =>
                    Right(d.defaultParams(witnessName).asInstanceOf[t])
                  case Some((witnessName, idx)) =>
                      Left((Nil, s"Cannot resolve `${witnessName}`"))
                  case None => Left(Nil, "Field not available")
              case Value.Arr(values) if values.isEmpty =>
                val (witnessName, idx) = labels.next()
                if d.defaultParams.contains(witnessName) then
                  Right(d.defaultParams(witnessName).asInstanceOf[t])
                else
                  Left(Nil, s"Cannot resolve `${witnessName}`")
              case _ =>
                val (witnessName,_) = labels.next()
                Left(Nil, s"Cannot resolve `${witnessName}`")

      val combineFields: Ap[[a] =>> Either[Parse.Error, a]] =
        [a, b] =>
          (ff: Either[Parse.Error, a => b], fa: Either[Parse.Error, a]) =>
            (fa, ff) match
                case (Left(e),Right(_)) => Left(e)
                case (Right(_),Left(e)) => Left(e)
                case (Right(a),Right(f)) => Right(f(a))
                case (Left((_, _)), Left((path, message))) => Left((path,message))

      inst.constructA(decodeField)(
        pure = [a] => (x: a) => Right(x),
        map = [a, b] => (fa: Either[Parse.Error, a], f: a => b) => fa.map(f),
        ap = combineFields
      )
