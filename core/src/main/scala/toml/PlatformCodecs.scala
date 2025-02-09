package toml

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

private[toml] trait PlatformCodecs {
  implicit val localDateCodec: Codec[LocalDate] = Codec {
    case (Value.Date(value), _, _) => Right(value)
    case (value, _, _) =>
      Left((List.empty, s"LocalDate expected, $value provided"))
  }

  implicit val localTimeCodec: Codec[LocalTime] = Codec {
    case (Value.Time(value), _, _) => Right(value)
    case (value, _, _) =>
      Left((List.empty, s"LocalTime expected, $value provided"))
  }

  implicit val localDateTimeCodec: Codec[LocalDateTime] = Codec {
    case (Value.DateTime(value), _, _) => Right(value)
    case (value, _, _) =>
      Left((List.empty, s"LocalDateTime expected, $value provided"))
  }

  implicit val offsetDateTimeCodec: Codec[OffsetDateTime] = Codec {
    case (Value.OffsetDateTime(value), _, _) => Right(value)
    case (value, _, _) =>
      Left((List.empty, s"OffsetDateTime expected, $value provided"))
  }
}
