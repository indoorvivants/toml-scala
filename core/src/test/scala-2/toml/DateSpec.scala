import java.time._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import fastparse.Parsed._
import toml._, derivation.auto._

class DateSpec extends AnyFunSuite with Matchers {
  import TestHelpers._

  test("Codec derivation") {
    case class Root(ld: LocalDate)

    val toml = "ld = 1979-05-27"
    assert(Toml.parseAs[Root](toml) == Right(Root(LocalDate.of(1979, 5, 27))))
  }
}
