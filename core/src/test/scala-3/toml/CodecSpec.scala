import org.scalatest.funsuite.AnyFunSuite
import toml._, derivation.auto._

trait CodecSpecExtras {self: CodecSpec =>

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
}
