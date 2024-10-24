# toml-scala

[![toml-scala Scala version support](https://index.scala-lang.org/indoorvivants/toml-scala/toml-scala/latest.svg)](https://index.scala-lang.org/indoorvivants/toml-scala/toml-scala)

toml-scala is a feature-complete implementation of [TOML](https://github.com/toml-lang/toml) for the Scala platform. It can parse TOML content into an AST or map it onto `case class` hierarchies. Furthermore, it can generate TOML back from an AST.

---

This project is an updated and maintained fork of the [original sparsetech/toml-scala](https://github.com/sparsetech/toml-scala/pull/24#issuecomment-2425882246)

---

## Features
- Standard-compliant
- AST parsing
- Codec derivation
    - Optional values
    - Custom codecs
- Generating TOML from ASTs
- Error handling
- Property-based unit tests

## Compatibility
| Back end     | Scala versions  | Date support | Tests         |
|:-------------|:----------------|:-------------|:--------------|
| JVM          | 2.12, 2.13, 3  | Yes          | Yes           |
| Scala.js      | 2.12, 2.13, 3  | Yes (1)      | Yes           |
| Scala Native | 2.12, 2.13, 3  | Yes (1)      | Yes |

- (1) On Scala.js and Scala Native we introduce an _optional_ dependency on https://index.scala-lang.org/cquiroz/scala-java-time. If your code doesn't use datetime codecs, then you don't need to add it to your build.
      Otherwise you need to explicitly depend on it, otherwise you will get linking errors

### Dependencies
```scala
libraryDependencies += "com.indoorvivants" %%  "toml" % "<version>"  // JVM
libraryDependencies += "com.indoorvivants" %%% "toml" % "<version>"  // Scala.js, Scala Native
```

## Examples
### AST parsing
```scala
toml.Toml.parse("a = 1")  // Right(Tbl(Map(a -> Num(1))))
```

### Codec derivation

**Currently only supported on Scala 2**

The following import is needed:

```scala
import toml.derivation.auto._
```

#### Tables
```scala
case class Table(b: Int)
case class Root(a: Int, table: Table)

val table =
  """
    |a = 1
    |[table]
    |b = 2
  """.stripMargin

Toml.parseAs[Root](table)  // Right(Root(1, Table(2)))
```

#### Table lists
```scala
val tableList =
  """
    |points = [ { x = 1, y = 2, z = 3 },
    |           { x = 7, y = 8, z = 9 },
    |           { x = 2, y = 4, z = 8 } ]
  """.stripMargin

case class Point(x: Int, y: Int, z: Int)
case class Root(points: List[Point])

Toml.parseAs[Root](tableList)
// Right(Root(List(
//   Point(1, 2, 3),
//   Point(7, 8, 9),
//   Point(2, 4, 8))))
```

Lists can be mapped onto `case class`es as a shorter alternative. Therefore, the following notation would yield the same result:

```toml
points = [ [ 1, 2, 3 ],
           [ 7, 8, 9 ],
           [ 2, 4, 8 ] ]
```

#### Optional values
```scala
case class Table(b: Int)
case class Root(a: Int, table: Option[Table])

Toml.parseAs[Root]("a = 1")  // Right(Root(1, None))
```

#### Define custom codecs
```scala
case class Currency(name: String)
implicit val currencyCodec: Codec[Currency] = Codec {
  case (Value.Str(value), _, _) =>
    value match {
      case "EUR" => Right(Currency("EUR"))
      case "BTC" => Right(Currency("BTC"))
      case _     => Left((List(), s"Invalid currency: $value"))
    }

  case (value, _, _) => Left((List(), s"Currency expected, $value provided"))
}

case class Root(currency: Currency)
Toml.parseAs[Root]("""currency = "BTC"""")  // Right(Root(Currency(BTC)))
```

#### Generate TOML
```scala
val root = Root(List(Pair("scalaDeps", Arr(List(
  Arr(List(Str("io.monix"), Str("minitest"), Str("2.2.2"))),
  Arr(List(Str("org.scalacheck"), Str("scalacheck"), Str("1.14.0"))),
  Arr(List(Str("org.scalatest"), Str("scalatest"), Str("3.2.0-SNAP10")))
)))))

Toml.generate(root)
```

Returns:

```toml
scalaDeps = [
  ["io.monix", "minitest", "2.2.2"],
  ["org.scalacheck", "scalacheck", "1.14.0"],
  ["org.scalatest", "scalatest", "3.2.0-SNAP10"]
]
```

#### Language Extensions
toml-scala supports the following language extensions which are disabled by default:

* [New lines and trailing commas in inline tables](https://github.com/toml-lang/toml/issues/516)

To enable them, pass in a set of extensions to the `parse()` or `parseAs()` function as a second argument:

```scala
toml.Toml.parse("""key = {
  a = 23,
  b = 42,
}""", Set(toml.Extension.MultiLineInlineTables))
```

## Links
* [ScalaDoc](https://www.javadoc.io/doc/com.indoorvivants/toml-scala_2.13/)

## Licence
toml-scala is licensed under the terms of the Mozilla Public Licence v2.0.

## Credits
The rules and their respective tests were derived from [stoml](https://github.com/jvican/stoml) by Jorge Vicente Cantero.

toml-scala is based on top of [FastParse](https://github.com/lihaoyi/fastparse) for defining the language rules. [ScalaCheck](https://github.com/rickynils/scalacheck) allows us to test synthetic examples of TOML files. The codec derivation was implemented using [Shapeless](https://github.com/milessabin/shapeless).

## Contributors
* Tim Nieradzik
