package dev.playo.room.room;

import dev.playo.room.AbstractPostgresContainerTest;
import dev.playo.room.TestUtils;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class RoomControllerIntegrationTest extends AbstractPostgresContainerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private BuildingRepository buildingRepository;

  @BeforeEach
  void clearDatabase() {
    roomRepository.deleteAll();
    buildingRepository.deleteAll();
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

  private RoomEntity createTestRoom(){
    BuildingEntity buildingEntity = createTestBuilding();
    BuildingEntity savedBuilding = buildingRepository.save(buildingEntity);

    List<Characteristic> characteristics = new ArrayList<>();
    characteristics.add(new Characteristic("Whiteboard", 1));
    characteristics.add(new Characteristic("Projector", 1));

    RoomEntity room = new RoomEntity();
    room.setName("TestRoom");
    room.setBuilding(savedBuilding);
    room.setCharacteristics(characteristics);

    return room;
  }

  private BuildingEntity createTestBuilding(){
    BuildingEntity buildingEntity = new BuildingEntity();

    buildingEntity.setName("testBuilding");
    buildingEntity.setDescription("testBuildingDescription");
    buildingEntity.setAddress("testBuildingAddress");
    buildingEntity.setState(BuildingState.OPEN);

    return buildingEntity;
  }
}
