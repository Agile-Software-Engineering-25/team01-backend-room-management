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
import dev.playo.room.building.data.BuildingEntity;
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
  
  private BuildingEntity testBuilding;

  @BeforeEach
  void clearDatabase() {
    this.testCleaner.clean();
    
    this.testBuilding = TestUtils.createTestBuilding(this.buildingRepository);
  }

  @Test
  void shouldCreateRoom() throws Exception {
    var room = TestUtils.createTestRoom(this.testBuilding);

    var request = new RoomCreateRequest(
      room.getName(),
      room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics(),
      List.of());

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
    RoomEntity room = TestUtils.createTestRoom(this.testBuilding);

    var request = new RoomCreateRequest(
      " " + room.getName(),
      room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics(),
      List.of());

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(this.objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenInvalidChemSymbol() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(this.testBuilding);

    var request = new RoomCreateRequest(
      room.getName(),
      " " + room.getChemSymbol(),
      room.getBuilding().getId(),
      room.getCharacteristics(),
      List.of());

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(this.objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldDeleteExistingRoom() throws Exception {
    RoomEntity room = TestUtils.createTestRoom(this.testBuilding);
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
    RoomEntity room = TestUtils.createTestRoom(this.testBuilding);
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
    var room = TestUtils.createTestRoom(this.testBuilding);
    this.roomRepository.save(room);

    this.mockMvc.perform(get("/rooms/{id}/deletable", room.getId())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deletable").value(true));
  }

  @Test
  void shouldReturnRoomIsNotDeletable() throws Exception {
    var room = TestUtils.createTestRoom(this.testBuilding);
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
    RoomEntity originalRoom = TestUtils.createTestRoom(this.testBuilding);
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
    RoomEntity existingRoom = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(existingRoom);

    // Create and persist second room that will attempt to use the same name
    RoomEntity targetRoom = TestUtils.createTestRoom(this.testBuilding, "testroom2", "Aurum");
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
    RoomEntity targetRoom = TestUtils.createTestRoom(this.testBuilding);
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

  @Test
  void shouldCreateCompositeRoomAndAssignChildren() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);

    var compositeRequest = new RoomCreateRequest(
      "compositeroom",
      "CompSym",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 80)),
      List.of(child1.getId(), child2.getId())
    );

    var result = mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compositeRequest)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("compositeroom"))
      .andExpect(jsonPath("$.composedOf").isArray())
      .andExpect(jsonPath("$.composedOf[0]").exists())
      .andReturn();

    var tree = objectMapper.readTree(result.getResponse().getContentAsString());
    var compositeId = UUID.fromString(tree.get("id").asText());

    var reloadedChild1 = roomRepository.findById(child1.getId()).orElseThrow();
    var reloadedChild2 = roomRepository.findById(child2.getId()).orElseThrow();
    assertThat(reloadedChild1.getParent()).isNotNull();
    assertThat(reloadedChild2.getParent()).isNotNull();
    assertThat(reloadedChild1.getParent().getId()).isEqualTo(compositeId);
    assertThat(reloadedChild2.getParent().getId()).isEqualTo(compositeId);
  }

  @Test
  void shouldFailCreatingCompositeRoomWhenChildAlreadyHasParent() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);
    var child3 = TestUtils.createTestRoom(this.testBuilding, "child3", "child3");
    roomRepository.save(child3);

    var firstCompositeRequest = new RoomCreateRequest(
      "compositeroom1",
      "CompSym1",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 50)),
      List.of(child1.getId(), child3.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(firstCompositeRequest)))
      .andExpect(status().isOk());

    var secondCompositeRequest = new RoomCreateRequest(
      "compositeroom2",
      "CompSym2",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 60)),
      List.of(child1.getId(), child2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(secondCompositeRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldFailCreatingCompositeRoomWhenChildIsComposite() throws Exception {
    var leaf1 = TestUtils.createTestRoom(this.testBuilding, "leaf1", "Leaf1");
    roomRepository.save(leaf1);
    var leaf2 = TestUtils.createTestRoom(this.testBuilding, "leaf2", "Leaf2");
    roomRepository.save(leaf2);
    var leaf3 = TestUtils.createTestRoom(this.testBuilding, "leaf3", "Leaf3");
    roomRepository.save(leaf3);

    var innerCompositeRequest = new RoomCreateRequest(
      "innercomp",
      "InnerSym",
      leaf1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 40)),
      List.of(leaf1.getId(), leaf2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(innerCompositeRequest)))
      .andExpect(status().isOk());

    var innerComposite = roomRepository.findAll().stream()
      .filter(r -> r.getName().equals("innercomp"))
      .findFirst().orElseThrow();

    var outerCompositeRequest = new RoomCreateRequest(
      "outercomp",
      "OuterSym",
      leaf1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 100)),
      List.of(innerComposite.getId(), leaf3.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(outerCompositeRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldUpdateCompositeRoomReplacingChildren() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);
    var child3 = TestUtils.createTestRoom(this.testBuilding, "child3", "child3");
    roomRepository.save(child3);
    var child4 = TestUtils.createTestRoom(this.testBuilding, "child4", "child4");
    roomRepository.save(child4);

    var compositeRequest = new RoomCreateRequest(
      "initialcomp",
      "InitSym",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 70)),
      List.of(child1.getId(), child2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compositeRequest)))
      .andExpect(status().isOk());

    var composite = roomRepository.findAll().stream()
      .filter(r -> r.getName().equals("initialcomp"))
      .findFirst().orElseThrow();

    var updateRequest = new RoomCreateRequest(
      "initialcomp",
      "InitSym",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 90)),
      List.of(child3.getId(), child4.getId())
    );

    mockMvc.perform(put("/rooms/{id}", composite.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.composedOf").isArray());

    var reChild1 = roomRepository.findById(child1.getId()).orElseThrow();
    var reChild2 = roomRepository.findById(child2.getId()).orElseThrow();
    var reChild3 = roomRepository.findById(child3.getId()).orElseThrow();
    var reChild4 = roomRepository.findById(child4.getId()).orElseThrow();

    assertThat(reChild1.getParent() == null).isTrue();
    assertThat(reChild2.getParent() == null).isTrue();
    assertThat(reChild3.getParent()).isNotNull();
    assertThat(reChild4.getParent()).isNotNull();
  }

  @Test
  void shouldFailUpdatingCompositeRoomWhenChildBelongsToAnotherComposite() throws Exception {
    var a1 = TestUtils.createTestRoom(this.testBuilding, "a1", "A1");
    roomRepository.save(a1);
    var a2 = TestUtils.createTestRoom(this.testBuilding, "a2", "A2");
    roomRepository.save(a2);
    var b1 = TestUtils.createTestRoom(this.testBuilding, "b1", "B1");
    roomRepository.save(b1);
    var b2 = TestUtils.createTestRoom(this.testBuilding, "b2", "B2");
    roomRepository.save(b2);

    var compARequest = new RoomCreateRequest(
      "compa",
      "CompASym",
      a1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 40)),
      List.of(a1.getId(), a2.getId())
    );
    var compBRequest = new RoomCreateRequest(
      "compb",
      "CompBSym",
      b1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 30)),
      List.of(b1.getId(), b2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compARequest)))
      .andExpect(status().isOk());
    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compBRequest)))
      .andExpect(status().isOk());

    var compA = roomRepository.findAll().stream().filter(r -> r.getName().equals("compa")).findFirst().orElseThrow();
    var updateCompA = new RoomCreateRequest(
      "compa",
      "CompASym",
      a1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 45)),
      List.of(a1.getId(), a2.getId(), b1.getId())
    );

    mockMvc.perform(put("/rooms/{id}", compA.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateCompA)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldDeleteCompositeRoomAndDetachChildren() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);

    var compositeRequest = new RoomCreateRequest(
      "todetelecomp",
      "DelSym",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 55)),
      List.of(child1.getId(), child2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compositeRequest)))
      .andExpect(status().isOk());

    var composite = roomRepository.findAll().stream()
      .filter(r -> r.getName().equals("todetelecomp"))
      .findFirst().orElseThrow();

    mockMvc.perform(delete("/rooms/{id}", composite.getId()))
      .andExpect(status().isNoContent());

    assertThat(roomRepository.findById(composite.getId())).isEmpty();
    assertThat(roomRepository.findById(child1.getId()).orElseThrow().getParent()).isNull();
    assertThat(roomRepository.findById(child2.getId()).orElseThrow().getParent()).isNull();
  }

  @Test
  void shouldDeleteCompositeRoomEvenIfChildrenBooked() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);

    var compositeRequest = new RoomCreateRequest(
      "compchild",
      "CBChild",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 60)),
      List.of(child1.getId(), child2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compositeRequest)))
      .andExpect(status().isOk());

    var booking = TestUtils.createTestBooking(child1);
    bookingRepository.save(booking);

    var composite = roomRepository.findAll().stream()
      .filter(r -> r.getName().equals("compchild"))
      .findFirst().orElseThrow();

    mockMvc.perform(delete("/rooms/{id}", composite.getId()))
      .andExpect(status().isNoContent());

    assertThat(roomRepository.findById(composite.getId())).isEmpty();
    assertThat(bookingRepository.findById(booking.getId())).isPresent();
    assertThat(roomRepository.findById(child1.getId()).orElseThrow().getParent()).isNull();
  }

  @Test
  void shouldFailDeletingChildRoomOfComposite() throws Exception {
    var child1 = TestUtils.createTestRoom(this.testBuilding);
    roomRepository.save(child1);
    var child2 = TestUtils.createTestRoom(this.testBuilding, "child2", "child2");
    roomRepository.save(child2);

    var compositeRequest = new RoomCreateRequest(
      "parentfordelete",
      "PFD",
      child1.getBuilding().getId(),
      List.of(new Characteristic("SEATS", 40)),
      List.of(child1.getId(), child2.getId())
    );

    mockMvc.perform(post("/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(compositeRequest)))
      .andExpect(status().isOk());

    mockMvc.perform(delete("/rooms/{id}", child1.getId()))
      .andExpect(status().isBadRequest());
  }
}
