package dev.playo.room.booking;

import static dev.playo.room.util.DateTimeNormalizer.toInstant;
import static dev.playo.room.util.DateTimeNormalizer.toLocalDateTime;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.config.BusinessConfiguration;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookingService {

  private final RoomService roomService;
  private final BookingRepository bookingRepository;
  private final BusinessConfiguration businessConfiguration;

  @Autowired
  public BookingService(
    @NonNull RoomService roomService,
    @NonNull BookingRepository bookingRepository,
    @NonNull BusinessConfiguration businessConfiguration
  ) {
    this.roomService = roomService;
    this.bookingRepository = bookingRepository;
    this.businessConfiguration = businessConfiguration;
  }

  public @NonNull Booking createBooking(@NonNull RoomBookingRequest request) {
    log.info(
      "Creating booking for room {} from {} to {}",
      request.getRoomId(),
      request.getStartTime(),
      request.getEndTime());

    //TODO: document
    var startTime = toLocalDateTime(request.getStartTime());
    var endTime = toLocalDateTime(request.getEndTime());
    if (startTime.isBefore(endTime)) {
      throw new GeneralProblemException(HttpStatus.BAD_REQUEST, "Start time must be before end time.");
    }

    // TODO: document
    if (startTime.isEqual(endTime)) {
      throw new GeneralProblemException(HttpStatus.BAD_REQUEST, "Cannot book a room without a duration.");
    }

    // TODO: document
    var days = ChronoUnit.DAYS.between(startTime, endTime);
    if (days > this.businessConfiguration.getMultiDayBookingMaxDays()) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Cannot book a room for more than %d days.".formatted(this.businessConfiguration.getMultiDayBookingMaxDays()));
    }

    if (startTime.toLocalTime().isBefore(this.businessConfiguration.getEarlyBookingTime())) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Cannot book a room before %s.".formatted(this.businessConfiguration.getEarlyBookingTime()));
    }

    if (endTime.toLocalTime().isAfter(this.businessConfiguration.getLateBookingTime())) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Cannot book a room after %s.".formatted(this.businessConfiguration.getLateBookingTime()));
    }

    var requestedRoom = this.roomService.findRoomById(request.getRoomId());
    var bookingEntity = new BookingEntity();
    bookingEntity.setRoom(requestedRoom);
    bookingEntity.setStartTime(toInstant(request.getStartTime()));
    bookingEntity.setEndTime(toInstant(request.getEndTime()));

    log.info(
      "Entering booking creation for room {} from {} to {}",
      requestedRoom.getName(),
      request.getStartTime(),
      request.getEndTime());
    try {
      var booking = this.bookingRepository.save(bookingEntity);
      log.info("Booking for room {} created with ID {}", requestedRoom.getName(), booking.getId());
      return booking.toBookingDto();
    } catch (DataIntegrityViolationException exception) {
      log.trace("Data integrity violation while creating booking: {}", exception.getMessage());
      throw new GeneralProblemException(
        HttpStatus.CONFLICT,
        "Booking for room %s from %s to %s overlaps with an existing booking.".formatted(
          requestedRoom.getName(),
          startTime,
          endTime));
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
