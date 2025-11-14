package dev.playo.room.integration.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.playo.generated.roommanagement.model.BuildingCreateRequest;
import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.room.AbstractPostgresContainerTest;
import dev.playo.room.booking.BookingService;
import dev.playo.room.building.BuildingService;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.integration.TestCleaner;
import dev.playo.room.room.RoomService;
import dev.playo.room.util.Characteristics;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock(@ConfigureWireMock(port = 9000))
@SpringBootTest
class BookingServiceTest extends AbstractPostgresContainerTest {

  @Autowired
  private BookingService bookingService;

  @Autowired
  private RoomService roomService;

  @Autowired
  private BuildingService buildingService;

  @Autowired
  private TestCleaner testCleaner;

  @AfterEach
  void tearDown() {
    this.testCleaner.clean();
  }

  @Test
  @DisplayName("createBooking saves booking and persists all data correctly")
  void createBookingPersistsAllDataCorrectly() {
    var buildingRequest = new BuildingCreateRequest();
    buildingRequest.setName("Test Building");
    buildingRequest.setDescription("Test Building");
    buildingRequest.setAddress("Test Address");
    var building = this.buildingService.createBuilding(buildingRequest);

    var roomRequest = new RoomCreateRequest();
    roomRequest.setName("Test Room");
    roomRequest.setChemSymbol("Hydrogenium");
    roomRequest.setBuildingId(building.getId());
    var seatsCharacteristic = new Characteristic();
    seatsCharacteristic.setType(Characteristics.SEATS_CHARACTERISTIC);
    seatsCharacteristic.setValue(10);
    roomRequest.setCharacteristics(List.of(seatsCharacteristic));

    var room = this.roomService.createRoom(roomRequest);

    var request = new RoomBookingRequest();
    request.setRoomId(room.getId());
    request.setStartTime(LocalDateTime.of(2024, 7, 1, 10, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 7, 1, 12, 0).atOffset(ZoneOffset.UTC));
    var lecturerId = UUID.randomUUID();
    var groupId = "GroupA";
    request.setLecturerIds(Set.of(lecturerId));
    request.setStudentGroupNames(Set.of(groupId));

    var booking = bookingService.createBooking(request);
    assertThat(booking.getRoomId()).isEqualTo(room.getId());
    assertThat(booking.getStartTime()).isEqualTo(request.getStartTime());
    assertThat(booking.getEndTime()).isEqualTo(request.getEndTime());
    assertThat(booking.getLecturerIds()).containsExactly(lecturerId);
    assertThat(booking.getStudentGroupNames()).containsExactly(groupId);
  }

  @Test
  @DisplayName("createBooking throws on duplicate booking (overlap)")
  void createBookingThrowsOnDuplicateBooking() {
    var buildingRequest = new BuildingCreateRequest();
    buildingRequest.setName("Test Building");
    buildingRequest.setDescription("Test Building");
    buildingRequest.setAddress("Test Address");
    var building = this.buildingService.createBuilding(buildingRequest);

    var roomRequest = new RoomCreateRequest();
    roomRequest.setName("Test Room");
    roomRequest.setChemSymbol("Hydrogenium");
    roomRequest.setBuildingId(building.getId());
    var seatsCharacteristic = new Characteristic();
    seatsCharacteristic.setType(Characteristics.SEATS_CHARACTERISTIC);
    seatsCharacteristic.setValue(10);
    roomRequest.setCharacteristics(List.of(seatsCharacteristic));

    var room = this.roomService.createRoom(roomRequest);

    var request1 = new RoomBookingRequest();
    request1.setRoomId(room.getId());
    request1.setStartTime(LocalDateTime.of(2024, 7, 1, 10, 0).atOffset(ZoneOffset.UTC));
    request1.setEndTime(LocalDateTime.of(2024, 7, 1, 12, 0).atOffset(ZoneOffset.UTC));
    request1.setLecturerIds(Set.of(UUID.randomUUID()));
    request1.setStudentGroupNames(Set.of("GroupA"));

    bookingService.createBooking(request1);

    var request2 = new RoomBookingRequest();
    request2.setRoomId(room.getId());
    request2.setStartTime(LocalDateTime.of(2024, 7, 1, 11, 0).atOffset(ZoneOffset.UTC));
    request2.setEndTime(LocalDateTime.of(2024, 7, 1, 13, 0).atOffset(ZoneOffset.UTC));
    request2.setLecturerIds(Set.of(UUID.randomUUID()));
    request2.setStudentGroupNames(Set.of("GroupA"));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request2));
    assertThat(ex.getMessage()).contains("overlaps with an existing booking");
  }

  @Test
  @DisplayName("createBooking of defect room")
  void createBookingOfDefectRoom() {
    var buildingRequest = new BuildingCreateRequest();
    buildingRequest.setName("Test Building");
    buildingRequest.setDescription("Test Building");
    buildingRequest.setAddress("Test Address");
    var building = this.buildingService.createBuilding(buildingRequest);

    var roomRequest = new RoomCreateRequest();
    roomRequest.setName("Test Room");
    roomRequest.setChemSymbol("Hydrogenium");
    roomRequest.setBuildingId(building.getId());
    var seatsCharacteristic = new Characteristic();
    seatsCharacteristic.setType(Characteristics.SEATS_CHARACTERISTIC);
    seatsCharacteristic.setValue(10);
    roomRequest.setCharacteristics(List.of(seatsCharacteristic));
    roomRequest.setDefects(List.of("defect"));

    var room = this.roomService.createRoom(roomRequest);

    var request1 = new RoomBookingRequest();
    request1.setRoomId(room.getId());
    request1.setStartTime(LocalDateTime.of(2024, 7, 1, 10, 0).atOffset(ZoneOffset.UTC));
    request1.setEndTime(LocalDateTime.of(2024, 7, 1, 12, 0).atOffset(ZoneOffset.UTC));
    request1.setLecturerIds(Set.of(UUID.randomUUID()));
    request1.setStudentGroupNames(Set.of("GroupA"));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request1));
    assertThat(ex.getMessage()).contains("Cannot book room marked as defective");
  }

}
