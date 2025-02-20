package toml

import java.time.*

import fastparse.*
import fastparse.NoWhitespace.*

private[toml] trait PlatformRules { this: Rules =>
  private val TenPowers =
    List(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

  def localTime[$: P]: P[Value.Time] = P(
    digit.rep(2).! ~ ":" ~ digit.rep(2).! ~ ":" ~ digit
      .rep(2)
      .! ~ ("." ~ digit.rep.!).?,
  ).map { case (h, m, s, ns) =>
    val nano = ns
      .map { str =>
        val digits = str.length
        str.toInt * TenPowers(9 - digits)
      }
      .getOrElse(0)

    Value.Time(LocalTime.of(h.toInt, m.toInt, s.toInt, nano))
  }

  def localDate[$: P]: P[Value.Date] = P(
    digit.rep(4).! ~ "-" ~ digit.rep(2).! ~ "-" ~ digit.rep(2).!,
  ).map { case (y, m, d) =>
    Value.Date(LocalDate.of(y.toInt, m.toInt, d.toInt))
  }

  def localDateTime[$: P]: P[Value.DateTime] = P(
    localDate ~ "T" ~ localTime,
  ).map { case (date, time) =>
    Value.DateTime(LocalDateTime.of(date.value, time.value))
  }

  def offsetDateTime[$: P]: P[Value.OffsetDateTime] = P(
    localDateTime ~ ("Z" | (("-" | "+") ~ digit.rep(2) ~ ":" ~ digit.rep(2))).!,
  ).map { case (dateTime, offset) =>
    // https://github.com/cquiroz/scala-java-time/issues/518
    val zone = offset match {
      case "Z"   => ZoneOffset.UTC
      case other => ZoneOffset.of(other)
    }
    Value.OffsetDateTime(OffsetDateTime.of(dateTime.value, zone))
  }

  def date[$: P] = P(offsetDateTime | localDateTime | localDate | localTime)
}
