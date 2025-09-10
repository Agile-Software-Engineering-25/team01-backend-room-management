package dev.playo.room.integration.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.RoomInquiry;
import dev.playo.generated.roommanagement.model.SearchCharacteristic;
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

import java.time.OffsetDateTime;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void clearDatabase() {
    this.testCleaner.clean();
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
  void shouldReturnEmptyList() throws Exception {
    var room = TestUtils.createTestRoom(this.buildingRepository);
    room.setCharacteristics(List.of(new Characteristic("Television", 1)));
    this.roomRepository.save(room);var c1 = new SearchCharacteristic("CurryCookie", 1, SearchCharacteristic.OperatorEnum.EQUALS);
    var body = new RoomInquiry(
      OffsetDateTime.now().plusHours(2),
      OffsetDateTime.now().plusHours(4),
      UUID.randomUUID(),
      List.of(c1)
    );

    this.mockMvc.perform(post("/rooms/inquiry")
        .content(objectMapper.writeValueAsString(body))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void shouldReturnRoomWithCharacteristic() throws Exception {
    var room = TestUtils.createTestRoom(this.buildingRepository);
    room.setCharacteristics(List.of(new Characteristic("Television", 1)));
    this.roomRepository.save(room);
    var c1 = new SearchCharacteristic("Television", 1, SearchCharacteristic.OperatorEnum.EQUALS);
    var body = new RoomInquiry(
      OffsetDateTime.now().plusHours(2),
      OffsetDateTime.now().plusHours(4),
      UUID.randomUUID(),
      List.of(c1)
    );

    this.mockMvc.perform(post("/rooms/inquiry")
        .content(objectMapper.writeValueAsString(body))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(room.getId().toString()));
  }
}
