package forex.domain

import cats.Show

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampShow: Show[Timestamp] = Show.show { t =>
    DateTimeFormatter
      .ofPattern("dd.MM.yyyy HH:mm:ss.SSSZ")
      .format(t.value)
  }
}
