package dev.playo.room.booking;

import dev.playo.room.booking.data.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookingCleanService {

  private final BookingRepository bookingRepository;

  @Autowired
  public BookingCleanService(BookingRepository bookingRepository) {
    this.bookingRepository = bookingRepository;
  }

  @Transactional
  @Scheduled(cron = "${room.booking.clean-cron:0 1/5 * * * *}")
  public void cleanOutdatedBookings() {
    log.debug("Cleaning outdated bookings...");
    var count = this.bookingRepository.deleteAllOutdatedBookings();
    log.info("Cleaned {} outdated bookings", count);
  }
}
