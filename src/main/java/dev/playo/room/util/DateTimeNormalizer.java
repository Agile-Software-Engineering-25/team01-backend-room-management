package dev.playo.room.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.NonNull;

public final class DateTimeNormalizer {

  /**
   * Converts the given {@link OffsetDateTime} to an {@link Instant} in the system default time zone, which in this
   * application is set to Europe/Berlin.
   *
   * @param offsetDateTime the offset date time to convert.
   * @return the instant at the Europe/Berlin time zone.
   */
  public static Instant toInstant(@NonNull OffsetDateTime offsetDateTime) {
    return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toInstant();
  }

  /**
   * Converts the given {@link OffsetDateTime} to a {@link LocalDateTime} in the system default time zone, which in this
   * application is set to Europe/Berlin.
   *
   * @param offsetDateTime the offset date time to convert.
   * @return the local date time at the Europe/Berlin time zone.
   */
  public static LocalDateTime toLocalDateTime(@NonNull OffsetDateTime offsetDateTime) {
    return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
  }

  /**
   * Converts the given instant back to an {@link OffsetDateTime} in the system default time zone, which in this
   * application is set to Europe/Berlin.
   *
   * @param instant the instant to convert.
   * @return the offset date time at the Europe/Berlin time zone.
   */
  public static @NonNull OffsetDateTime fromInstant(@NonNull Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
