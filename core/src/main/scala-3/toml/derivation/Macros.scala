package toml

import deriving.*, quoted.*, compiletime.*
import toml.Codec.Defaults

private[toml] object Macros {

  def summonInstances[T: Type, Elems: Type](using
      Quotes
  ): List[Expr[Codec[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] =>
        deriveOrSummon[T, elem].asInstanceOf :: summonInstances[T, elems]
      case '[EmptyTuple] => Nil

  def summonLabels[Elems: Type](using Quotes): List[Expr[String]] =
    Type.of[Elems] match
      case '[elem *: elems] =>
        val expr = Expr.summon[ValueOf[elem]].get

        '{ $expr.value.asInstanceOf[String] } :: summonLabels[elems]
      case '[EmptyTuple] => Nil

  def deriveOrSummon[T: Type, Elem: Type](using
      Quotes
  ): Expr[Codec[Elem]] =
    Type.of[Elem] match
      case '[T] => deriveRec[T, Elem]
      case _    => '{ summonInline[Codec[Elem]] }

  def deriveRec[T: Type, Elem: Type](using
      Quotes
  ): Expr[Codec[Elem]] =
    Type.of[T] match
      case '[Elem] => '{ error("infinite recursive derivation") }
      case _       => derivedMacro[Elem] // recursive derivation

  def derivedMacro[T: Type](using Quotes): Expr[Codec[T]] =
    val ev: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].get

    import quotes.reflect.*

    ev match
      case '{
            $m: Mirror.ProductOf[T] {
              type MirroredElemTypes = elementTypes;
              type MirroredElemLabels = labels
              type MirroredLabel = commandName
            }
          } =>
        val instances = summonInstances[T, elementTypes]
        val labels = summonLabels[labels]

        val codecs =
          Expr.ofList(
            labels
              .zip(instances)
              .map((label, inst) => Expr.ofTuple(label -> inst))
          )

        '{
          new Codec[T] {
            override def apply(
                value: Value,
                defaults: Defaults,
                index: Int
            ): Either[Parse.Error, T] =
              value match
                case Value.Tbl(values) =>
                  import util.boundary
                  var tpl: Tuple = EmptyTuple
                  boundary:
                    $codecs.reverse.foreach: (label, codec) =>
                      values.get(label) match
                        case None =>
                          defaults.get(label) match
                            case None =>
                              boundary.break(
                                Left(Nil -> s"Field `$label` is missing")
                              )
                            case Some(defaultValue) =>
                              tpl = defaultValue *: tpl

                        case Some(value) =>
                          codec.apply(value, defaults, index) match
                            case Left(a) => boundary.break(Left(a))
                            case Right(decodedValue) =>
                              tpl = decodedValue *: tpl

                    Right($m.fromProduct(tpl))

                case _ =>
                  Left(
                    Nil -> s"Table expected, ${value.getClass().getSimpleName()} provided"
                  )
          }
        }

      case '{
            $m: Mirror.SumOf[T] {
              type MirroredElemTypes = elementTypes;
              type Morrore
            }
          } =>
        val instances = Expr.ofList(summonInstances[T, elementTypes])

        '{
          new Codec[T] {
            override def apply(
                value: Value,
                defaults: Defaults,
                index: Int
            ): Either[Parse.Error, T] =
              import util.boundary
              boundary:
                $instances.foreach: codec =>
                  codec.apply(value, defaults, index) match
                    case Left(value) =>
                    case Right(value) =>
                      boundary.break(Right(value.asInstanceOf[T]))
                Left(Nil -> "Could not parse value")

              // sys.error("what?")
          }
        }
}
