package toml
package derivation
import shapeless3.deriving.*
import toml.Codec.Defaults

trait DerivedProductCodec[P] extends Codec[P]
object DerivedProductCodec:
  given codecGen[P](using
      inst: K0.ProductInstances[Codec, P],
      labelling: Labelling[P],
      d: DefaultParams[P],
  ): DerivedProductCodec[P] with

    def unfold[T](
        defaults: Defaults,
        ctx: Context,
        codec: Codec[T],
    ): (Context, Option[T]) =
      val head = ctx.head

      def fail(f: Context => Context) =
        f(ctx) -> None

      ctx.tomlValue match
        case Value.Tbl(map) if map.contains(head) =>
          codec(map(head), defaults, 0) match
            case Right(t) =>
              (
                Context(Value.Tbl(map - head), ctx.tail, 0, ctx.error),
                Some(t),
              )
            case Left((path, e)) =>
              (ctx.withError(head :: path, e), None)
        case Value.Tbl(map) =>
          map.keySet.diff(labelling.elemLabels.toSet).headOption match
            case Some(unknown) =>
              fail(
                _.withError(unknown :: ctx.errorAddress, s"Unknown field"),
              )
            case None =>
              d.defaultParams.get(head) match
                case None =>
                  if codec.optional then
                    (ctx.continueWithTail, Some(None.asInstanceOf[T]))
                  else
                    fail(
                      _.withErrorMessage(s"Cannot resolve `${head}`"),
                    )
                case Some(t) =>
                  (ctx.continueWithTail, Some(t.asInstanceOf[T]))
        case Value.Arr(values @ (head +: tail)) =>
          codec(head, defaults, ctx.index) match
            case Left((path, e)) =>
              fail(
                _.withError(s"#${ctx.index + 1}" :: path, e),
              )
            case Right(t) =>
              (
                Context(
                  Value.Arr(tail),
                  ctx.tail,
                  ctx.index + 1,
                  (ctx.errorAddress, ctx.errorMessage),
                ),
                Some(t),
              )
        case Value.Arr(Nil) =>
          d.defaultParams.get(head) match
            case None =>
              if codec.optional then
                (
                  ctx.continueWithTail,
                  Some(None.asInstanceOf[T]),
                )
              else
                fail(
                  _.withErrorMessage(
                    s"Cannot resolve `$head`",
                  ),
                )
            case Some(t) =>
              (ctx.continueWithTail, Some(t.asInstanceOf[T]))
        case _ =>
          fail(
            _.withError(Nil, s"Cannot resolve `$head`"),
          )
      end match
    end unfold

    def apply(
        value: Value,
        defaults: Defaults,
        index: Int,
    ): Either[Parse.Error, P] =
      val result = inst.unfold(Context(value, labelling.elemLabels, index)):
        [t] =>
          (ctx: Context, codec: Codec[t]) => unfold[t](defaults, ctx, codec)

      result match
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
