import org.scalatest.funsuite.AnyFunSuite
import toml._, derivation.auto._

trait CodecSpecExtras {self: CodecSpec =>

  // test("Table (5)") {
  //   case class Table(b: Int)
  //   case class Root(a: Int, table: Option[Table])

  //   val table = "a = 1"
  //   assert(Toml.parseAs[Root](table) == Right(Root(1, None)))
  // }

  test("Inline list of tuples with default values (2)") {
    val tableList = """points = [ [ 1, "2" ], [ 7 ], [ ] ]"""

    case class Point(x: Int = 23, y: Option[String])
    case class Root(points: List[Point])

    // assert(Toml.parseAs[Root](tableList) == Right(Root(List(
    //   Point(1, Some("2")),
    //   Point(7, None),
    //   Point(23, None)))))
  }

  test("Inline list of tuples with default values (3)") {
    val tableList = """points = [ [ 1, "2" ], [ 7 ], [ ] ]"""

    case class Point(x: Option[Int] = Some(23), y: Option[String])
    case class Root(points: List[Point])

    // assert(Toml.parseAs[Root](tableList) == Right(Root(List(
    //   Point(Some(1), Some("2")),
    //   Point(Some(7), None),
    //   Point(Some(23), None)))))
  }

  test("Inline list of tuples with default values (4)") {
    val tableList = """points = [ [ 1, "2" ] ]"""

    case class Point(x: Option[Int] = Some(23))
    case class Root(points: List[Point])

    // assert(Toml.parseAs[Root](tableList) == Left((List("points", "#1"),
    //   "Too many elements; remove Str(2)")))
  }

  test("Inline list of tuples with default values (5)") {
    val tableList = """points = [ [ 1, "2" ], [ ], [ 3, 4 ] ]"""

    case class Point(x: Int = 23, y: Option[String])
    case class Root(points: List[Point])

    // assert(Toml.parseAs[Root](tableList) ==
    //   Left((List("points", "#3", "#2"), "String expected, Num(4) provided")))
  }

  test("Array of tables (2)") {
    case class Product(name  : Option[String] = Option.empty,
                       sku   : Option[Int]    = Option.empty,
                       colour: Option[String] = Option.empty)
    case class Root(products: List[Product])

    val array =
      """
        |[[products]]
        |name = "Hammer"
        |sku = 738594937
        |
        |[[products]]
        |
        |[[products]]
        |name = "Nail"
        |sku = 284758393
        |colour = "grey"
      """.stripMargin

    // assert(Toml.parseAs[Root](array) == Right(Root(List(
    //   Product(Some("Hammer"), Some(738594937), None),
    //   Product(None, None, None),
    //   Product(Some("Nail"), Some(284758393), Some("grey"))))))
  }

  test("Array of tables (3)") {
    case class Physical(colour: String, shape: String)
    case class Variety(name: String)
    case class Fruit(name: String,
                     physical: Option[Physical],
                     variety: List[Variety])
    case class Root(fruit: List[Fruit])

    val array =
      """
        |[[fruit]]
        |  name = "apple"
        |
        |  [fruit.physical]
        |    colour = "red"
        |    shape  = "round"
        |
        |  [[fruit.variety]]
        |    name = "red delicious"
        |
        |  [[fruit.variety]]
        |    name = "granny smith"
        |
        |[[fruit]]
        |  name = "banana"
        |
        |  [[fruit.variety]]
        |    name = "plantain"
      """.stripMargin

  //   assert(Toml.parseAs[Root](array) == Right(Root(List(
  //     Fruit("apple", Some(Physical("red", "round")), List(
  //       Variety("red delicious"),
  //       Variety("granny smith")
  //     )),
  //     Fruit("banana", None, List(Variety("plantain")))))))
  }


  test("Error handling (6)") {
    // Despite of the default value, an error must be triggered
    case class Module(a: Option[Module] = None,
                      b: List[List[Int]] = List())
    case class Root(module: Map[String, Module])
    val toml =
      """
        |[module.name.a]
        |b = [1, 2, 3]
      """.stripMargin
    // assert(Toml.parseAs[Root](toml) ==
    //        Left((
    //          List("module", "name", "a", "b", "#1"),
    //          "List expected, Num(1) provided")))
  }

  test("Default parameters (5)") {
    case class A(value1: Option[String], value2: Int = 42)
    case class Root(a: A)
    val toml = "a = { }"
    //assert(Toml.parseAs[Root](toml) == Right(Root(A(None, 42))))
  }
}
