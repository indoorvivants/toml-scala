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
      inst.unfold[Context](
        Context(value, labelling.elemLabels, index)
      )(
        [t] =>
          (ctx: Context, codec: Codec[t]) =>
            val head = ctx.head
            ctx.tomlValue match
              case Value.Tbl(map) if map.contains(head) =>
                codec(map(head), defaults, 0) match
                  case Right(t) =>
                    (
                      Context(Value.Tbl(map - head), ctx.tail, 0, ctx.error),
                      Some(t)
                    )
                  case Left((path, e)) =>
                    (ctx.withError(head :: path, e), None)
              case Value.Tbl(map) =>
                map.keySet.diff(labelling.elemLabels.toSet).headOption match
                  case None =>
                    d.defaultParams.get(head) match
                      case None =>
                        if codec.optional then
                          (ctx.continueWithTail, Some(None.asInstanceOf[t]))
                        else
                          (
                            ctx.withErrorMessage(s"Cannot resolve `${head}`"),
                            None
                          )
                      case Some(t) =>
                        (ctx.continueWithTail, Some(t.asInstanceOf[t]))
                  case Some(unknown) =>
                    (
                      ctx.withError(
                        unknown :: ctx.errorAddress,
                        s"Unknown field"
                      ),
                      None
                    )
              case Value.Arr(values @ (head +: tail)) =>
                codec(head, defaults, ctx.index) match
                  case Left((path, e)) =>
                    (
                      ctx.withError(s"#${ctx.index + 1}" :: path, e),
                      None
                    )
                  case Right(t) =>
                    (
                      Context(
                        Value.Arr(tail),
                        ctx.tail,
                        ctx.index + 1,
                        (ctx.errorAddress, ctx.errorMessage)
                      ),
                      Some(t)
                    )
              case Value.Arr(Nil) =>
                d.defaultParams.get(head) match
                  case None =>
                    if codec.optional then
                      (
                        ctx.continueWithTail,
                        Some(None.asInstanceOf[t])
                      )
                    else
                      (
                        ctx.withErrorMessage(
                          s"Cannot resolve `$head`"
                        ),
                        None
                      )
                  case Some(t) =>
                    (ctx.continueWithTail, Some(t.asInstanceOf[t]))
              case _ =>
                (
                  ctx.withError(Nil, s"Cannot resolve `$head`"),
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
  * Codec derivation is stateful operation in that
  *   - it removes an entry from TOML table when it successfully parse a field
  *   - it traverses TOML array by mutable index
  *   - it accumulates error address as a list
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
