package dev.playo.room.booking;

import dev.playo.generated.roommanagement.api.BookingsApi;
import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Controller
public class BookingController implements BookingsApi {

  private final BookingService bookingService;

  @Autowired
  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  public ResponseEntity<Booking> bookRoom(RoomBookingRequest roomBookingRequest) {
    return ResponseEntity.ok(this.bookingService.createBooking(roomBookingRequest));
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  public ResponseEntity<Void> cancelBookingById(UUID bookingId) {
    this.bookingService.cancelBooking(bookingId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  public ResponseEntity<List<Booking>> getAllBookings() {
    return ResponseEntity.ok(this.bookingService.allKnownBookings());
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  public ResponseEntity<Booking> getBookingById(UUID bookingId) {
    var booking = this.bookingService.findBooking(bookingId);
    return ResponseEntity.ok(booking.toBookingDto());
  }
}
