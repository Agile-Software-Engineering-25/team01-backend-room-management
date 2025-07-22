package dev.playo.room.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.NonNull;

public final class DateTimeNormalizer {

  public static Instant toInstant(@NonNull OffsetDateTime offsetDateTime) {
    return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toInstant();
  }

  public static OffsetDateTime fromInstant(@NonNull Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
