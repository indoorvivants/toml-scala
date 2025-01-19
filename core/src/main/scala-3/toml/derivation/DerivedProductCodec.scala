package toml
package derivation
import toml.Codec.Defaults
import toml.Codec.Index
import shapeless3.deriving.*

trait DerivedProductCodec[P] extends Codec[P]
object DerivedProductCodec:
  given codecGen[P](using
      inst: K0.ProductInstances[Codec, P],
      labelling: Labelling[P],
      d: DefaultParams[P]
  ): DerivedProductCodec[P] with
    def apply(
        value: Value,
        defaults: Defaults,
        index: Int
    ): Either[Parse.Error, P] =
      type Acc = Context
      inst.unfold[Acc](
        Context(value, labelling.elemLabels, index)
      )(
        [t] =>
          (acc: Acc, codec: Codec[t]) =>
            val head = acc.head
            acc.tomlValue match
              case Value.Tbl(map) if map.contains(head) =>
                codec(map(head), defaults, 0) match
                  case Right(t) =>
                    (
                      Context(Value.Tbl(map - head), acc.tail, 0, acc.error),
                      Some(t)
                    )
                  case Left((path, e)) =>
                    (acc.withError(head :: path, e), None)
              case Value.Tbl(map) =>
                map.keySet.diff(labelling.elemLabels.toSet).headOption match
                  case None =>
                    d.defaultParams.get(head) match
                      case None =>
                        if codec.optional then
                          (acc.continueWithTail, Some(None.asInstanceOf[t]))
                        else
                          (
                            acc.withErrorMessage(s"Cannot resolve `${head}`"),
                            None
                          )
                      case Some(t) =>
                        (acc.continueWithTail, Some(t.asInstanceOf[t]))
                  case Some(unknown) =>
                    (
                      acc.withError(
                        unknown :: acc.errorAddress,
                        s"Unknown field"
                      ),
                      None
                    )
              case Value.Arr(values @ (head +: tail)) =>
                codec(head, defaults, acc.index) match
                  case Left((path, e)) =>
                    (
                      acc.withError(s"#${acc.index + 1}" :: path, e),
                      None
                    )
                  case Right(t) =>
                    (
                      Context(
                        Value.Arr(tail),
                        acc.tail,
                        acc.index + 1,
                        (acc.errorAddress, acc.errorMessage)
                      ),
                      Some(t)
                    )
              case Value.Arr(Nil) =>
                d.defaultParams.get(head) match
                  case None =>
                    if codec.optional then
                      (
                        acc.continueWithTail,
                        Some(None.asInstanceOf[t])
                      )
                    else
                      (
                        acc.withErrorMessage(
                          s"Cannot resolve `$head`"
                        ),
                        None
                      )
                  case Some(t) =>
                    (acc.continueWithTail, Some(t.asInstanceOf[t]))
              case _ =>
                (
                  acc.withError(Nil, s"Cannot resolve `$head`"),
                  None
                )
            end match
      ) match
        case (Context(Value.Tbl(map), _, _, (path, e)), p) =>
          if map.isEmpty then p.toRight((path, e))
          else
            map.keySet.diff(labelling.elemLabels.toSet).headOption match
              case None    => p.toRight((path, e))
              case Some(f) => Left(((List(f), s"Unknown field")))
        case (Context(Value.Arr(elems), _, _, (path, e)), p) =>
          if elems.isEmpty then p.toRight((path, e))
          else if e.isEmpty() then
            Left((path, s"Too many elements; remove ${elems.head}"))
          else Left((path, e))
        case (Context(_, _, _, (path, e)), _) => Left((path, e))
      end match
    end apply
  end codecGen
end DerivedProductCodec

/** This type represents derivation context.
  *
  * @param tomlValue
  *   the TOML value from which the value of type `P` is built
  * @param labels
  *   field names of type `P`
  * @param index
  *   index of array elements
  * @param error
  *   a pair of error address and error message.
  */
private final case class Context(
    tomlValue: Value,
    labels: Seq[String],
    index: Int,
    error: Parse.Error
):
  def head = labels.head
  def tail = labels.tail
  def errorAddress = error._1
  def errorMessage = error._2

  def withErrorMessage(message: String) = copy(
    error = (errorAddress, message)
  )
  def withError(address: List[String], message: String) = copy(
    error = (address, message)
  )
  def continueWithTail = copy(
    tomlValue,
    tail,
    index,
    error
  )
end Context

private object Context:
  def apply(
      tomlValue: Value,
      labels: Seq[String],
      index: Int
  ) = new Context(tomlValue, labels, index, (Nil, ""))
