package dev.playo.room;

import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.room.data.RoomEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class TestUtils {

  private TestUtils(){}

  public static RoomEntity createTestRoom(BuildingEntity building) {
    return createTestRoom(building, "testroom", "Hydrogenium");
  }

  public static RoomEntity createTestRoom(BuildingEntity building, String name, String chemSymbol) {
    List<Characteristic> characteristics = new ArrayList<>();
    characteristics.add(new Characteristic("SEATS", 30));
    characteristics.add(new Characteristic("Projector", 1));

    RoomEntity room = new RoomEntity();
    room.setName(name);
    room.setChemSymbol(chemSymbol);
    room.setBuilding(building);
    room.setCharacteristics(characteristics);

    return room;
  }

  public static BuildingEntity createTestBuilding(BuildingRepository buildingRepository) {
    BuildingEntity buildingEntity = new BuildingEntity();

    buildingEntity.setName("testBuilding");
    buildingEntity.setDescription("testBuildingDescription");
    buildingEntity.setAddress("testBuildingAddress");

    return buildingRepository.save(buildingEntity);
  }

  public static BookingEntity createTestBooking(RoomEntity room) {
    Set<UUID> lecturerIds = new HashSet<>();
    lecturerIds.add(UUID.randomUUID());
    Set<UUID> studentGroupIds = new HashSet<>();
    studentGroupIds.add(UUID.randomUUID());

    Instant currentStart = Instant.now();
    Instant currentEnd = currentStart.plus(1, ChronoUnit.HOURS);

    BookingEntity bookingEntity = new BookingEntity();
    bookingEntity.setRoom(room);
    bookingEntity.setStartTime(currentStart);
    bookingEntity.setEndTime(currentEnd);
    bookingEntity.setLecturerIds(lecturerIds);
    bookingEntity.setStudentGroupIds(studentGroupIds);

    return bookingEntity;
  }
}
