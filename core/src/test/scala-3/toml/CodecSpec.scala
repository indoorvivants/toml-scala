import org.scalatest.funsuite.AnyFunSuite
import toml._, derivation.auto._

trait CodecSpecExtras {self: CodecSpec =>


  test("Inline list of tuples with default values (4)") {
    val tableList = """points = [ [ 1, "2" ] ]"""

    case class Point(x: Option[Int] = Some(23))
    case class Root(points: List[Point])

    // assert(Toml.parseAs[Root](tableList) == Left((List("points", "#1"),
    //   "Too many elements; remove Str(2)")))
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
