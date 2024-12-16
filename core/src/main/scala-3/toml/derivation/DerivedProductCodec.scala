package toml
package derivation
import toml.Codec.Defaults
import toml.Codec.Index
import shapeless3.deriving.*

trait DerivedProductCodec[P] extends Codec[P]
object DerivedProductCodec:
  given codecGen[P](
    using inst: K0.ProductInstances[Codec, P],labelling: Labelling[P], d:DefaultParams[P]
  ): DerivedProductCodec[P]  with
    def apply(value: Value, defaults: Defaults, index: Int): Either[Parse.Error, P] =
      type Acc = (Value, (/*labels: */Seq[String], Index), Parse.Error)
      inst.unfold[Acc]((value, (labelling.elemLabels, index), (Nil, "")))(
        [t] => (acc : Acc, codec: Codec[t]) =>
          val (value, (labels @ (head +: tail), index), (path, e)) = acc : @unchecked
          value match
            case Value.Tbl(map) if map.contains(head) =>
              codec(map(head), defaults, 0) match
                case Right(t) => ((Value.Tbl(map - head),(tail,0),(path,e)), Some(t))
                case Left((path,e)) => ((value, (labels,index), (head :: path, e)), None)
            case Value.Tbl(map) =>
              map.keySet.diff(labelling.elemLabels.toSet).headOption match
                case None =>
                  d.defaultParams.get(head) match
                    case None =>
                      if codec.optional then
                        ((value,(tail, index),(path,e)), Some(None.asInstanceOf[t]))
                      else
                        ((value, (labels,index), (path, s"Cannot resolve `${head}`")),None)
                    case Some(t) =>
                      ((value,(tail, index),(path,e)), Some(t.asInstanceOf[t]))
                case Some(unknown) =>
                  ((value, (labels, index), (unknown :: path, s"Unknown field")), None)
            case Value.Arr(values @ (head +: tail)) =>
              codec(head, defaults, index) match
                case Left((path,e)) => ((value, (labels, index), (s"#${index + 1}" :: path, e)),None)
                case Right(t) => ((Value.Arr(tail), (labels.tail, index + 1), (path,e)),Some(t))
            case Value.Arr(Nil) =>
              d.defaultParams.get(head) match
                case None =>
                  if codec.optional then
                    ((value,(tail, index),(path,e)), Some(None.asInstanceOf[t]))
                  else
                    ((value, (labels,index), (path, s"Cannot resolve `$head`")),None)
                case Some(t) =>
                  ((value,(tail, index),(path,e)), Some(t.asInstanceOf[t]))
            case _ =>
              ((value,(labels, index),(Nil, s"Cannot resolve `$head`")), None)
      ) match
        case ((Value.Tbl(map), _,(path, e)), p) =>
          if map.isEmpty then p.toRight((path ,e))
          else
            map.keySet.diff(labelling.elemLabels.toSet).headOption
              match
              case None => p.toRight((path,e))
              case Some(f) => Left(((List(f), s"Unknown field")))
        case ((Value.Arr(elems),_,(path, e)),p)  =>
          if elems.isEmpty then p.toRight((path,e))
          else if e.isEmpty() then Left((path, s"Too many elements; remove ${elems.head}"))
          else Left((path,e))
        case ((_,_,(path,e)),_) => Left((path,e))
