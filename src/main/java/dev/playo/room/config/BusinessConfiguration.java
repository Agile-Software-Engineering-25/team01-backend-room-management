package dev.playo.room.config;

import java.time.LocalTime;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BusinessConfiguration {

  /**
   * Across how many days can a room be booked. One indicates that a room can only be booked for one day.
   */
  @Value("${room.booking.max-days:0}")
  private int multiDayBookingMaxDays;

  @Value("${room.booking.early-booking:06:00:00}")
  private LocalTime localTime;

}
