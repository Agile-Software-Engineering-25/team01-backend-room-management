package dev.playo.room.room;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.generated.roommanagement.model.RoomInquiry;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.room.data.RoomRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final RoomRepository repository;
  private final BookingRepository bookingRepository;

  @Autowired
  public RoomService(@NonNull RoomRepository repository, BookingRepository bookingRepository) {
    this.repository = repository;
    this.bookingRepository = bookingRepository;
  }

  public @NonNull Room createRoom(@NonNull RoomCreateRequest room) {
    var lowerCaseName = room.getName().toLowerCase();
    if (this.repository.existsByName(lowerCaseName)) {
      throw new GeneralProblemException(HttpStatus.BAD_REQUEST,
        "Room with name %s already exists".formatted(lowerCaseName));
    }

    var roomEntity = new RoomEntity();
    roomEntity.setName(lowerCaseName);
    roomEntity.setLocatedAt(room.getLocatedAt());
    var savedRoom = this.repository.save(roomEntity);
    return savedRoom.toRoomDto();
  }

  public @NonNull RoomEntity findRoomById(@NonNull UUID roomId) {
    var room = this.repository.findById(roomId).orElse(null);
    if (room == null) {
      throw new GeneralProblemException(HttpStatus.NOT_FOUND, "Room with ID %s does not exist".formatted(roomId));
    }

    return room;
  }

  public @NonNull List<Room> findAvailableRooms(@NonNull RoomInquiry roomInquiry) {
    return this.repository.findAvailableRooms(
        roomInquiry.getStartTime().toInstant(),
        roomInquiry.getEndTime().toInstant(),
        10)
      .stream()
      .map(RoomEntity::toRoomDto)
      .toList();
  }

  public @NonNull List<Room> allKnownRooms() {
    return this.repository.findAll()
      .stream()
      .map(RoomEntity::toRoomDto)
      .toList();
  }

  public @NonNull List<Booking> findBookingsByRoomAndDate(@NonNull UUID roomId, @NonNull LocalDate date) {
    var room = this.findRoomById(roomId);
    return this.bookingRepository.findBookingByRoomAndDate(room, date)
      .stream()
      .map(booking -> new Booking()
        .id(booking.getId())
        .roomId(booking.getRoom().getId())
        .startTime(this.instantToOffsetDateTime(booking.getStartTime()))
        .endTime(this.instantToOffsetDateTime(booking.getEndTime()))
        .lecturerId(UUID.randomUUID())
        .studentGroupId(UUID.randomUUID()))
      .toList();
  }

  public @NonNull Room updateRoom(@NonNull UUID roomId, @NonNull RoomCreateRequest room) {
    var existingRoom = this.findRoomById(roomId);
    var lowerCaseName = room.getName().toLowerCase();
    if (!existingRoom.getName().equals(lowerCaseName) && this.repository.existsByName(lowerCaseName)) {
      throw new GeneralProblemException(HttpStatus.BAD_REQUEST,
        "Room with name %s already exists".formatted(lowerCaseName));
    }

    existingRoom.setName(lowerCaseName);
    existingRoom.setLocatedAt(room.getLocatedAt());
    var updatedRoom = this.repository.save(existingRoom);
    return updatedRoom.toRoomDto();
  }

  public void deleteRoomById(@NonNull UUID roomId) {
    var room = this.findRoomById(roomId);
    this.repository.delete(room);
  }

  private @NonNull OffsetDateTime instantToOffsetDateTime(@NonNull Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
