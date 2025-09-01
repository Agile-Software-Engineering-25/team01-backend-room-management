package dev.playo.room.booking;

import dev.playo.room.booking.data.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public final class BookingCleanService {

  private final BookingRepository bookingRepository;

  @Autowired
  public BookingCleanService(BookingRepository bookingRepository) {
    this.bookingRepository = bookingRepository;
  }

  @Scheduled(cron = "${room.booking.clean-cron:0 * * * * *}")
  public void cleanOutdatedBookings() {
    this.bookingRepository.deleteAllOutdatedBookings();
  }
}
