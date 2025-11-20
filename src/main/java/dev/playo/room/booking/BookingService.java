package dev.playo.room.booking;

import static dev.playo.room.util.DateTimeNormalizer.toInstant;
import static dev.playo.room.util.DateTimeNormalizer.toLocalDateTime;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.booking.data.allocation.BookingAllocation;
import dev.playo.room.booking.data.allocation.BookingAllocationId;
import dev.playo.room.config.BusinessConfiguration;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.student.StudentGroupClient;
import dev.playo.room.util.Characteristics;
import jakarta.transaction.Transactional;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
  private final StudentGroupClient studentGroupClient;
  private final BusinessConfiguration businessConfiguration;

  @Autowired
  public BookingService(
    @NonNull RoomService roomService,
    @NonNull BookingRepository bookingRepository,
    @NonNull StudentGroupClient studentGroupClient,
    @NonNull BusinessConfiguration businessConfiguration
  ) {
    this.roomService = roomService;
    this.bookingRepository = bookingRepository;
    this.studentGroupClient = studentGroupClient;
    this.businessConfiguration = businessConfiguration;
  }

  @Transactional
  public @NonNull Booking createBooking(@NonNull RoomBookingRequest request) {
    log.info(
      "Creating booking for room {} from {} to {}",
      request.getRoomId(),
      request.getStartTime(),
      request.getEndTime());

    //TODO: document
    var startTime = toLocalDateTime(request.getStartTime());
    var endTime = toLocalDateTime(request.getEndTime());
    if (startTime.isAfter(endTime)) {
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

    if (request.getStudentGroupNames().isEmpty() && request.getGroupSize() == null) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Either student group IDs or group size must be provided.");
    }

    var requestedRoom = this.roomService.findRoomById(request.getRoomId());
    if (requestedRoom.getDefects() != null && !requestedRoom.getDefects().isEmpty()) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Cannot book room marked as defective"
      );
    }
    if (requestedRoom.getComposedOf() != null && !requestedRoom.getComposedOf().isEmpty()) {
      for (var composed : requestedRoom.getComposedOf()) {
        if (composed.getDefects() != null && !composed.getDefects().isEmpty()) {
          throw new GeneralProblemException(
            HttpStatus.BAD_REQUEST,
            "Cannot book room with defective childroom"
          );
        }
      }
    }
    var availableSeats = requestedRoom.getCharacteristics()
      .stream()
      .filter(characteristic -> characteristic.getType().equals(Characteristics.SEATS_CHARACTERISTIC))
      .map(characteristic -> (int) characteristic.getValue())
      .findAny()
      .orElse(null);
    if (availableSeats == null) {
      log.error("Room {}:{} does not have a seats characteristic defined.",
        requestedRoom.getName(),
        requestedRoom.getId());
      throw new GeneralProblemException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Room %s does not have a seats characteristic defined.".formatted(requestedRoom.getName()));
    }

    this.ensureEnoughSeatsPresent(availableSeats, request.getGroupSize(), request.getStudentGroupNames());

    var startInstant = toInstant(request.getStartTime());
    var endInstant = toInstant(request.getEndTime());

    var bookingEntity = new BookingEntity();
    bookingEntity.setRoom(requestedRoom);
    bookingEntity.setStartTime(startInstant);
    bookingEntity.setEndTime(endInstant);
    bookingEntity.setLecturerIds(request.getLecturerIds());
    bookingEntity.setStudentGroupIds(request.getStudentGroupNames());

    List<RoomEntity> composedRooms = new ArrayList<>();
    composedRooms.add(requestedRoom);
    composedRooms.addAll(requestedRoom.getComposedOf());

    var parentRoom = requestedRoom.getParent();
    if (parentRoom != null) {
      composedRooms.add(parentRoom);
    }

    for (var roomToAllocate : composedRooms) {
      var allocation = new BookingAllocation();
      var allocationId = new BookingAllocationId();

      allocation.setId(allocationId);
      allocation.setRoom(roomToAllocate);
      allocation.setBooking(bookingEntity);
      allocation.setStartTime(startInstant);
      allocation.setEndTime(endInstant);

      bookingEntity.getAllocations().add(allocation);
    }

    log.info(
      "Entering booking creation for room {} from {} to {}",
      requestedRoom.getName(),
      request.getStartTime(),
      request.getEndTime());
    try {
      var booking = this.bookingRepository.saveAndFlush(bookingEntity);
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

  private void ensureEnoughSeatsPresent(int availableSeats, Integer groupSize, Set<String> studentGroupNames) {
    if (groupSize != null && groupSize > availableSeats) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Not enough seats in the room for the requested group size of %d.".formatted(groupSize));
    }

    if (groupSize == null && !studentGroupNames.isEmpty()) {
      int size = 0;
      for (var groupName : studentGroupNames) {
        var studentGroup = this.studentGroupClient.getStudentGroupByName(groupName);
        size += studentGroup == null ? 0 : studentGroup.studentsCount();
      }

      if (size > availableSeats) {
        throw new GeneralProblemException(
          HttpStatus.BAD_REQUEST,
          "Not enough seats in the room for the requested student groups.");
      }
    }
  }
}
