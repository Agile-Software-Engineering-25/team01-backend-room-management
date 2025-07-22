package dev.playo.room.room;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.generated.roommanagement.model.RoomInquiry;
import dev.playo.generated.roommanagement.model.SearchCharacteristic;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.room.data.RoomRepository;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final EntityManager entityManager;
  private final RoomRepository repository;
  private final BookingRepository bookingRepository;
  private final BuildingRepository buildingRepository;

  @Autowired
  public RoomService(
    EntityManager entityManager,
    @NonNull RoomRepository repository,
    BookingRepository bookingRepository,
    BuildingRepository buildingRepository
  ) {
    this.entityManager = entityManager;
    this.repository = repository;
    this.bookingRepository = bookingRepository;
    this.buildingRepository = buildingRepository;
  }

  public @NonNull Room createRoom(@NonNull RoomCreateRequest room) {
    var lowerCaseName = room.getName().toLowerCase();
    if (this.repository.existsByName(lowerCaseName)) {
      throw new GeneralProblemException(HttpStatus.BAD_REQUEST,
        "Room with name %s already exists".formatted(lowerCaseName));
    }

    var roomEntity = new RoomEntity();
    roomEntity.setName(lowerCaseName);
    //TODO: might need to validate building existence
    roomEntity.setBuilding(this.buildingRepository.getReferenceById(room.getBuildingId()));
    roomEntity.setCharacteristics(room.getCharacteristics());
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

  public List<Room> findAvailableRooms(RoomInquiry request) {
    var sql = new StringBuilder("""
      SELECT r.* FROM rooms r WHERE r.id NOT IN (SELECT b.room_id FROM bookings b
      WHERE b.start_time < :endTime AND b.end_time > :startTime)
      """);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("startTime", request.getStartTime());
    parameters.put("endTime", request.getEndTime());

    int index = 0;
    if (request.getCharacteristics() != null) {
      for (var characteristic : request.getCharacteristics()) {
        var typeParam = "type" + index;
        var valueParam = "value" + index;
        var value = characteristic.getValue();

        sql.append(" AND EXISTS (")
          .append("SELECT 1 FROM jsonb_array_elements(r.characteristics) elem ")
          .append("WHERE elem->> 'type' = :").append(typeParam)
          .append(" AND ");

        var operator = this.operatorForCharacteristic(characteristic.getOperator(), value);

        switch (value) {
          case Boolean _ -> sql.append("(elem->>'value')::boolean ").append(operator).append(" :").append(valueParam);
          case Integer _ -> sql.append("(elem->>'value')::int ").append(operator).append(" :").append(valueParam);
          case Map<?, ?> _ -> throw new GeneralProblemException(
            HttpStatus.BAD_REQUEST,
            "Complex objects are not supported as characteristic values.");
          case null -> sql.append("elem->>'value' IS NULL");
          default -> sql.append("elem->>'value' ").append(operator).append(" :").append(valueParam);
        }

        sql.append(")");

        parameters.put(typeParam, characteristic.getType().name());
        parameters.put(valueParam, value);
        index++;
      }
    }

    var query = this.entityManager.createNativeQuery(sql.toString(), RoomEntity.class);
    parameters.forEach(query::setParameter);

    List<RoomEntity> entities = query.getResultList();
    return entities.stream().map(RoomEntity::toRoomDto).toList();
  }

  public @NonNull List<Room> findRoomsByBuildingId(@NonNull UUID buildingId) {
    return this.repository.findRoomEntityByBuildingId(buildingId)
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
      .map(BookingEntity::toBookingDto)
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
    // TODO: might need to validate building existence
    existingRoom.setBuilding(this.buildingRepository.getReferenceById(room.getBuildingId()));
    var updatedRoom = this.repository.save(existingRoom);
    return updatedRoom.toRoomDto();
  }

  public void deleteRoomById(@NonNull UUID roomId) {
    var room = this.findRoomById(roomId);
    this.repository.delete(room);
  }

  private @NonNull String operatorForCharacteristic(
    @NonNull SearchCharacteristic.OperatorEnum operator,
    @Nullable Object value
  ) {
    return switch (operator) {
      case EQUALS -> "=";
      case NOT_EQUALS -> "<>";
      case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> {
        if (value instanceof Integer) {
          yield switch (operator) {
            case GREATER_THAN -> ">";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_OR_EQUAL -> "<=";
            default -> throw new GeneralProblemException(
              HttpStatus.BAD_REQUEST,
              "Unsupported operator %s for integer values.".formatted(operator));
          };
        } else {
          throw new GeneralProblemException(
            HttpStatus.BAD_REQUEST,
            "Operator %s is not supported for value type %s.".formatted(
              operator,
              value == null ? "null" : value.getClass().getSimpleName()));
        }
      }
    };
  }
}
