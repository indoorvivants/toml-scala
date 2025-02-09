package toml

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime as JOffsetDateTime

private[toml] trait PlatformValue {
  case class Date(value: LocalDate) extends Value
  case class Time(value: LocalTime) extends Value
  case class DateTime(value: LocalDateTime) extends Value
  case class OffsetDateTime(value: JOffsetDateTime) extends Value
}
