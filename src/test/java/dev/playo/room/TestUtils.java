package dev.playo.room;

import dev.playo.generated.roommanagement.model.BuildingState;
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

  public static RoomEntity createTestRoom(BuildingRepository buildingRepository){
    BuildingEntity buildingEntity = createTestBuilding();
    BuildingEntity savedBuilding = buildingRepository.save(buildingEntity);

    List<Characteristic> characteristics = new ArrayList<>();
    characteristics.add(new Characteristic("SEATS", 30));
    characteristics.add(new Characteristic("Projector", 1));

    RoomEntity room = new RoomEntity();
    room.setName("testroom");
    room.setChemSymbol("Hydrogenium");
    room.setBuilding(savedBuilding);
    room.setCharacteristics(characteristics);

    return room;
  }

  /**
   * A second version of createTestRoom
   * Sometimes a second room is needed, but you can't call createTestRoom twice as it causes key errors
   */
  public static RoomEntity createTestRoom2(BuildingRepository buildingRepository){
    BuildingEntity buildingEntity = createTestBuilding2();
    BuildingEntity savedBuilding = buildingRepository.save(buildingEntity);

    List<Characteristic> characteristics = new ArrayList<>();
    characteristics.add(new Characteristic("SEATS", 30));
    characteristics.add(new Characteristic("Projector", 1));

    RoomEntity room = new RoomEntity();
    room.setName("testroom2");
    room.setChemSymbol("Aurum");
    room.setBuilding(savedBuilding);
    room.setCharacteristics(characteristics);

    return room;
  }

  public static BuildingEntity createTestBuilding(){
    BuildingEntity buildingEntity = new BuildingEntity();

    buildingEntity.setName("testBuilding");
    buildingEntity.setDescription("testBuildingDescription");
    buildingEntity.setAddress("testBuildingAddress");

    return buildingEntity;
  }

  /**
   * A second version of createTestBuilding
   * Sometimes a second building is needed, but you can't call createTestBuilding twice as it causes key errors
   * Also used for createTestRoom2 because in createTestRoom createTestBuilding is used
   */
  public static BuildingEntity createTestBuilding2(){
    BuildingEntity buildingEntity = new BuildingEntity();

    buildingEntity.setName("testBuilding2");
    buildingEntity.setDescription("testBuilding2Description");
    buildingEntity.setAddress("testBuilding2Address");

    return buildingEntity;
  }

  public static BookingEntity createTestBooking(RoomEntity room){
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
