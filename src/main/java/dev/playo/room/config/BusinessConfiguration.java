package dev.playo.room.config;

import java.time.LocalTime;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BusinessConfiguration {

  /**
   * Across how many days can a room be booked. Zero indicates that the booking needs to start and finish on the same
   * day.
   */
  @Value("${room.booking.max-days:0}")
  private int multiDayBookingMaxDays;

  @Value("${room.booking.early-limit:06:00:00}")
  private LocalTime earlyBookingTime;

  @Value("${room.booking.late-limit:22:00:00}")
  private LocalTime lateBookingTime;
}
