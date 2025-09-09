package dev.playo.room.integration.room;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class RoomControllerIntegrationTest extends AbstractPostgresContainerTest {

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

  private final ObjectMapper mapper = new ObjectMapper();;

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
      .content(mapper.writeValueAsString(request))
      .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("testroom"))
      .andExpect(jsonPath("$.chemSymbol").value("hydrogenium"))
      .andExpect(jsonPath("$.buildingId").value(room.getBuilding().getId().toString()))
      .andExpect(jsonPath("$.characteristics").isArray())
      .andExpect(jsonPath("$.characteristics[0].type").value("Whiteboard"))
      .andExpect(jsonPath("$.characteristics[0].value").value(1))
      .andExpect(jsonPath("$.characteristics[1].type").value("Projector"))
      .andExpect(jsonPath("$.characteristics[1].value").value(1));

    List<RoomEntity> rooms = roomRepository.findAll();
    assertThat(rooms).hasSize(1);
    RoomEntity savedRoom = rooms.getFirst();
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
  void shouldReturnInvalidSymbolWhenCreatingRoom() throws Exception {
//    var room = TestUtils.createTestRoom(this.buildingRepository);
//
//    var request = new RoomCreateRequest();
//    request.setName("Test Room");
//    request.setChemSymbol("invalidSymbol:'#*+");
//    request.setBuildingId(room.getBuilding().getId());
//
//    this.mockMvc.perform(post("/rooms")
//      .content(mapper.writeValueAsString(request))
//      .contentType(MediaType.APPLICATION_JSON));
  }
}
