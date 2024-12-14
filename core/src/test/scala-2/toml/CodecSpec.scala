import org.scalatest.funsuite.AnyFunSuite
import toml._, derivation.auto._

trait CodecSpecExtras {self: CodecSpec =>

  test("Inline list of tuples with default values (4)") {
    val tableList = """points = [ [ 1, "2" ] ]"""

    case class Point(x: Option[Int] = Some(23))
    case class Root(points: List[Point])

    assert(Toml.parseAs[Root](tableList) == Left((List("points", "#1"),
      "Too many elements; remove Str(2)")))
  }
}
