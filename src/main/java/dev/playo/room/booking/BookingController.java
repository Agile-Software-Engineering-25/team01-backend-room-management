package dev.playo.room.booking;

import dev.playo.generated.roommanagement.api.BookingsApi;
import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class BookingController implements BookingsApi {

  private final BookingService bookingService;

  @Autowired
  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @Override
  public ResponseEntity<Booking> bookRoom(RoomBookingRequest roomBookingRequest) {
    return null;
  }

  @Override
  public ResponseEntity<Void> cancelBookingById(UUID bookingId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Booking>> getAllBookings() {
    return null;
  }

  @Override
  public ResponseEntity<Booking> getBookingById(UUID bookingId) {
    return null;
  }
}
