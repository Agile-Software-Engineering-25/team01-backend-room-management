package dev.playo.room.integration.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.room.AbstractPostgresContainerTest;
import dev.playo.room.TestUtils;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.integration.TestCleaner;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.room.data.RoomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerIntegrationTest extends AbstractPostgresContainerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestCleaner testCleaner;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private BuildingRepository buildingRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void clearDatabase() {
    this.testCleaner.clean();
  }

  @Test
  void shouldCreateRoom() throws Exception {
    var room = TestUtils.createTestRoom(buildingRepository);

    var request = new RoomCreateRequest(
      room.getName(),
      room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics());

    mockMvc.perform(post("/rooms")
        .content(this.objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(room.getName().toLowerCase()))
      .andExpect(jsonPath("$.chemSymbol").value(room.getChemSymbol().toLowerCase()))
      .andExpect(jsonPath("$.buildingId").value(room.getBuilding().getId().toString()))
      .andExpect(jsonPath("$.characteristics").isArray())
      .andExpect(jsonPath("$.characteristics[0].type").value("SEATS"))
      .andExpect(jsonPath("$.characteristics[0].value").value(30))
      .andExpect(jsonPath("$.characteristics[1].type").value("Projector"))
      .andExpect(jsonPath("$.characteristics[1].value").value(1));

    List<RoomEntity> rooms = roomRepository.findAll();
    assertThat(rooms).hasSize(1);
  }

  @Test
  void shouldReturn400WhenInvalidRoomName() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(buildingRepository);

    var request = new RoomCreateRequest(
      " " + room.getName(),
      room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics());

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(this.objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenInvalidChemSymbol() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(buildingRepository);

    var request = new RoomCreateRequest(
      room.getName(),
      " " + room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics());

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(this.objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldDeleteExistingRoom() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(buildingRepository);
    roomRepository.save(room);

    mockMvc.perform(delete("/rooms/{id}", room.getId())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    Optional<RoomEntity> deletedRoom = roomRepository.findById(room.getId());
    assertThat(deletedRoom).isEmpty();
  }

  @Test
  void shouldReturn404WhenDeletingNonExistingRoom() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/rooms/{id}", id)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn400WhenDeletingRoomWithInvalidId() throws Exception {
    String invalidId = "invalid-uuid";

    mockMvc.perform(delete("/rooms/{id}", invalidId)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void deleteBookedRoom() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(buildingRepository);
    roomRepository.save(room);

    BookingEntity bookingEntity = TestUtils.createTestBooking(room);
    bookingRepository.save(bookingEntity);

    mockMvc.perform(delete("/rooms/{id}", room.getId())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());

    Optional<RoomEntity> deletedRoom = roomRepository.findById(room.getId());
    Optional<BookingEntity> booking = bookingRepository.findById(bookingEntity.getId());

    assertThat(deletedRoom).isNotEmpty();
    assertThat(booking).isNotEmpty();

    mockMvc.perform(delete("/rooms/{id}", room.getId())
        .queryParam("force", "true")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful());

    assertThat(roomRepository.findById(room.getId())).isEmpty();
    assertThat(bookingRepository.findById(bookingEntity.getId())).isEmpty();
  }

  @Test
  void shouldReturnRoomIsDeletable() throws Exception {
    var room = TestUtils.createTestRoom(this.buildingRepository);
    this.roomRepository.save(room);

    this.mockMvc.perform(get("/rooms/{id}/deletable", room.getId())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deletable").value(true));
  }

  @Test
  void shouldReturnRoomIsNotDeletable() throws Exception {
    var room = TestUtils.createTestRoom(this.buildingRepository);
    this.roomRepository.save(room);

    var bookingEntity = TestUtils.createTestBooking(room);
    this.bookingRepository.save(bookingEntity);

    this.mockMvc.perform(get("/rooms/{id}/deletable", room.getId())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deletable").value(false));
  }

  @Test
  void shouldReturn404WhenCheckingDeletableForNonExistingRoom() throws Exception {
    var id = UUID.randomUUID();

    this.mockMvc.perform(get("/rooms/{id}/deletable", id)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void shouldUpdateRoomSuccessfully() throws Exception {
    // Create and persist original room with its building
    RoomEntity originalRoom = TestUtils.createTestRoom(buildingRepository);
    roomRepository.save(originalRoom);

    UUID buildingId = originalRoom.getBuilding().getId();

    // Prepare update request with new name, new building ID, and new characteristics
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("updatedroom");
    updateRequest.setChemSymbol("Aurum");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(
      new Characteristic("SEATS", 30),
      new Characteristic("SpeakerSystem", 2)
    ));

    // Perform PUT request to update the room
    mockMvc.perform(put("/rooms/{id}", originalRoom.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("updatedroom"))
      .andExpect(jsonPath("$.buildingId").value(buildingId.toString()))
      .andExpect(jsonPath("$.characteristics").isArray())
      .andExpect(jsonPath("$.characteristics[0].type").value("SEATS"))
      .andExpect(jsonPath("$.characteristics[1].type").value("SpeakerSystem"));

    // Verify that room was updated in the database
    Optional<RoomEntity> updatedRoomOpt = roomRepository.findById(originalRoom.getId());
    assertThat(updatedRoomOpt).isPresent();

    RoomEntity updatedRoom = updatedRoomOpt.get();
    assertThat(updatedRoom.getName()).isEqualTo("updatedroom");
    assertThat(updatedRoom.getBuilding().getId()).isEqualTo(buildingId);
    assertThat(updatedRoom.getCharacteristics()).containsExactlyInAnyOrder(
      new Characteristic("SEATS", 30),
      new Characteristic("SpeakerSystem", 2)
    );
  }

  @Test
  void shouldRejectUpdateWhenRoomNameAlreadyExists() throws Exception {
    // Create and persist first room
    RoomEntity existingRoom = TestUtils.createTestRoom(buildingRepository);
    roomRepository.save(existingRoom);

    // Create and persist second room that will attempt to use the same name
    RoomEntity targetRoom = TestUtils.createTestRoom2(buildingRepository);
    roomRepository.save(targetRoom);

    // Prepare update request with duplicate name (case-insensitive match)
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("testroom"); // same name as existingRoom, but lowercase
    updateRequest.setChemSymbol("Aurum");
    updateRequest.setBuildingId(targetRoom.getBuilding().getId());
    updateRequest.setCharacteristics(List.of(
      new Characteristic("SEATS", 30)
    ));

    // Perform PUT request to update the room
    mockMvc.perform(put("/rooms/{id}", targetRoom.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail").value("Room with name testroom already exists"));

    // Verify that the target room was not updated
    Optional<RoomEntity> unchangedRoomOpt = roomRepository.findById(targetRoom.getId());
    assertThat(unchangedRoomOpt).isPresent();

    RoomEntity unchangedRoom = unchangedRoomOpt.get();
    assertThat(unchangedRoom.getName()).isEqualTo("testroom2"); // original name remains
  }

  @Test
  void shouldRejectUpdateWhenSeatsCharacteristicIsMissing() throws Exception {
    // Create and persist a room to be updated
    RoomEntity targetRoom = TestUtils.createTestRoom(buildingRepository);
    roomRepository.save(targetRoom);

    // Prepare update request WITHOUT "Seats" characteristic
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("updatedroom");
    updateRequest.setChemSymbol("Aurum");
    updateRequest.setBuildingId(targetRoom.getBuilding().getId());
    updateRequest.setCharacteristics(List.of(
      new Characteristic("Whiteboard", 1),
      new Characteristic("Projector", 1)
    ));

    // Perform PUT request to update the room
    mockMvc.perform(put("/rooms/{id}", targetRoom.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail").value("Rooms need to have at least one SEAT"));

    // Verify that the room was not updated
    Optional<RoomEntity> unchangedRoomOpt = roomRepository.findById(targetRoom.getId());
    assertThat(unchangedRoomOpt).isPresent();

    RoomEntity unchangedRoom = unchangedRoomOpt.get();
    assertThat(unchangedRoom.getName()).isEqualTo("testroom"); // original name remains
  }
}
