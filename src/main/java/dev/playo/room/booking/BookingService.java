package dev.playo.room.booking;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

  private final RoomService roomService;
  private final BookingRepository bookingRepository;

  @Autowired
  public BookingService(RoomService roomService, BookingRepository bookingRepository) {
    this.roomService = roomService;
    this.bookingRepository = bookingRepository;
  }

  public @NonNull Booking createBooking(@NonNull RoomBookingRequest request) {
    var requestedRoom = this.roomService.findRoomById(request.getRoomId());
    var bookingEntity = new BookingEntity();
    bookingEntity.setRoom(requestedRoom);
    bookingEntity.setStartTime(request.getStartTime().toInstant());
    bookingEntity.setEndTime(request.getEndTime().toInstant());

    try {
      return this.bookingRepository.save(bookingEntity).toBookingDto();
    } catch (DataIntegrityViolationException exception) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Booking for room %s from %s to %s overlaps with an existing booking.".formatted(
          requestedRoom.getName(),
          request.getStartTime(),
          request.getEndTime()));
    }
  }

  public @NonNull BookingEntity findBooking(@NonNull UUID bookingId) {
    var booking = this.bookingRepository.findById(bookingId).orElse(null);
    if (booking == null) {
      throw new GeneralProblemException(
        HttpStatus.NOT_FOUND,
        "Booking with ID %s does not exist.".formatted(bookingId));
    }

    return booking;
  }

  public @NonNull List<Booking> allKnownBookings() {
    return this.bookingRepository.findAll().stream()
      .map(BookingEntity::toBookingDto)
      .toList();
  }

  public void cancelBooking(@NonNull UUID bookingId) {
    var booking = this.findBooking(bookingId);
    this.bookingRepository.delete(booking);
  }
}
