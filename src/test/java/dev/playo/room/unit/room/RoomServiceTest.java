package dev.playo.room.unit.room;

import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.room.data.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

  @Mock
  private EntityManager entityManager;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  BookingRepository bookingRepository;

  @Mock
  BuildingRepository buildingRepository;

  @InjectMocks
  private RoomService roomService;

  @Test
  void deleteRoomWithNonExistingId() {
    UUID id = UUID.randomUUID();

    assertThrows(GeneralProblemException.class, () -> roomService.deleteRoomById(id, false));
    verify(roomRepository, never()).deleteById(any());
  }

  @Test
  void deleteRoomByIdOfExistingRoom() {
    UUID roomId = UUID.randomUUID();
    RoomEntity mockRoom = new RoomEntity();
    mockRoom.setId(roomId);

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

    doNothing().when(roomRepository).delete(mockRoom);

    roomService.deleteRoomById(roomId, false);

    verify(roomRepository, times(1)).findById(roomId);

    verify(roomRepository, times(1)).delete(mockRoom);
  }

  @Test
  void roomWithoutBookingsShouldBeDeletable() {
    UUID roomId = UUID.randomUUID();
    RoomEntity room = new RoomEntity();
    room.setId(roomId);
    room.setName("testRoom");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(bookingRepository.existsCurrentOrFutureBookingForRoom(room)).thenReturn(false);

    boolean result = roomService.deletableRoom(roomId);

    verify(roomRepository).findById(roomId);
    verify(bookingRepository).existsCurrentOrFutureBookingForRoom(room);
    assertTrue(result);
  }

  @Test
  void roomWithBookingsShouldNotBeDeletable() {
    UUID roomId = UUID.randomUUID();
    RoomEntity room = new RoomEntity();
    room.setId(roomId);
    room.setName("testRoom");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(bookingRepository.existsCurrentOrFutureBookingForRoom(room)).thenReturn(true);

    boolean result = roomService.deletableRoom(roomId);
    verify(roomRepository).findById(roomId);
    verify(bookingRepository).existsCurrentOrFutureBookingForRoom(room);
    assertFalse(result);
  }

  @Test
  void deleteRoomWithForceDeleteShouldRemoveAllBookingsThenRoom() {
    UUID roomId = UUID.randomUUID();
    RoomEntity mockRoom = new RoomEntity();
    mockRoom.setId(roomId);
    mockRoom.setName("testRoom");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

    roomService.deleteRoomById(roomId, true);

    verify(roomRepository).findById(roomId);
    verify(bookingRepository).deleteAllByRoom(mockRoom);
    verify(roomRepository).delete(mockRoom);
  }

  @Test
  void deleteRoomWithoutForceDeleteAndWithBookingsShouldThrowException() {
    UUID roomId = UUID.randomUUID();
    RoomEntity mockRoom = new RoomEntity();
    mockRoom.setId(roomId);
    mockRoom.setName("testRoom");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    when(bookingRepository.existsCurrentOrFutureBookingForRoom(mockRoom)).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.deleteRoomById(roomId, false));

    verify(roomRepository).findById(roomId);
    verify(bookingRepository).existsCurrentOrFutureBookingForRoom(mockRoom);
    verify(roomRepository, never()).delete(any());
  }

  @Test
  void deleteRoomWithDataIntegrityViolationShouldThrowException() {
    UUID roomId = UUID.randomUUID();
    RoomEntity mockRoom = new RoomEntity();
    mockRoom.setId(roomId);
    mockRoom.setName("testRoom");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    when(bookingRepository.existsCurrentOrFutureBookingForRoom(mockRoom)).thenReturn(false);
    doThrow(new DataIntegrityViolationException("Constraint violation")).when(roomRepository).delete(mockRoom);

    assertThrows(GeneralProblemException.class, () -> roomService.deleteRoomById(roomId, false));

    verify(roomRepository).findById(roomId);
    verify(bookingRepository).existsCurrentOrFutureBookingForRoom(mockRoom);
  }

  @Test
  void deletableRoomShouldThrowExceptionWhenRoomDoesNotExist() {
    UUID roomId = UUID.randomUUID();

    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    assertThrows(GeneralProblemException.class, () -> roomService.deletableRoom(roomId));

    verify(roomRepository).findById(roomId);
    verify(bookingRepository, never()).existsCurrentOrFutureBookingForRoom(any());
  }

  @Test
  void updateRoomShouldChangeName() {
    // Setup
    UUID roomId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID buildingId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    List<Characteristic> characteristics = List.of(
      new Characteristic("Seats", 1),
      new Characteristic("Projector", 1)
    );

    BuildingEntity buildingEntity = new BuildingEntity();
    buildingEntity.setId(buildingId);

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    existingRoom.setBuilding(buildingEntity);
    existingRoom.setCharacteristics(characteristics);

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("newroomname");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(characteristics);

    RoomEntity savedRoom = new RoomEntity();
    savedRoom.setId(roomId);
    savedRoom.setName("newroomname");
    savedRoom.setBuilding(buildingEntity);
    savedRoom.setCharacteristics(characteristics);

    // Mocking
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName("newroomname")).thenReturn(false);
    when(buildingRepository.existsById(buildingId)).thenReturn(true);
    when(buildingRepository.getReferenceById(buildingId)).thenReturn(buildingEntity);
    when(roomRepository.save(any(RoomEntity.class))).thenReturn(savedRoom);

    // Execution
    Room result = roomService.updateRoom(roomId, updateRequest);

    // Verification
    verify(roomRepository).findById(roomId);
    verify(roomRepository).existsByName("newroomname");
    verify(buildingRepository).existsById(buildingId);
    verify(buildingRepository).getReferenceById(buildingId);
    verify(roomRepository).save(any(RoomEntity.class));

    // Assertion
    assertEquals("newroomname", result.getName());
  }

  @Test
  void updateRoomShouldChangeCharacteristic() {
    // Setup
    UUID roomId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID buildingId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    BuildingEntity buildingEntity = new BuildingEntity();
    buildingEntity.setId(buildingId);

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    existingRoom.setBuilding(buildingEntity);
    existingRoom.setCharacteristics(new ArrayList<>());

    List<Characteristic> newCharacteristics = List.of(
      new Characteristic("Seats", 1),
      new Characteristic("Projector", 1)
    );

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(newCharacteristics);

    RoomEntity savedRoom = new RoomEntity();
    savedRoom.setId(roomId);
    savedRoom.setName("newroomname");
    savedRoom.setBuilding(buildingEntity);
    savedRoom.setCharacteristics(newCharacteristics);

    // Mocking
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName("newroomname")).thenReturn(false);
    when(buildingRepository.existsById(buildingId)).thenReturn(true);
    when(buildingRepository.getReferenceById(buildingId)).thenReturn(buildingEntity);
    when(roomRepository.save(any(RoomEntity.class))).thenReturn(savedRoom);

    // Execution
    Room result = roomService.updateRoom(roomId, updateRequest);

    // Verification
    verify(roomRepository).findById(roomId);
    verify(roomRepository).existsByName("newroomname");
    verify(buildingRepository).existsById(buildingId);
    verify(buildingRepository).getReferenceById(buildingId);
    verify(roomRepository).save(any(RoomEntity.class));

    // Assertion
    assertEquals("newroomname", result.getName());
    assertEquals(newCharacteristics, result.getCharacteristics());
  }


  @Test
  void updateRoomShouldThrowExceptionWhenBuildingDoesNotExist() {
    UUID roomId = UUID.randomUUID();
    UUID oldBuildingId = UUID.randomUUID();
    UUID newBuildingId = UUID.randomUUID(); // different from existing

    // Existing room setup
    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    BuildingEntity oldBuilding = new BuildingEntity();
    oldBuilding.setId(oldBuildingId);
    existingRoom.setBuilding(oldBuilding);

    // Update request with new building ID
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setBuildingId(newBuildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("Seats", 30)));

    // Mocking
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName("newroomname")).thenReturn(false); // room already exists
    when(buildingRepository.existsById(newBuildingId)).thenReturn(false); // building does not exist

    // Expect exception
    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, updateRequest));

    // Verify interactions
    verify(roomRepository).findById(roomId);
    verify(roomRepository).existsByName("newroomname");
    verify(buildingRepository).existsById(newBuildingId);
    verifyNoMoreInteractions(roomRepository, buildingRepository);
  }

  @Test
  void updateRoomShouldThrowExceptionWhenRoomNotFound() {
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("Seats", 20)));

    // Mock: room not found
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    // Expect exception
    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, updateRequest));

    // Verify only findById was called
    verify(roomRepository).findById(roomId);
    verifyNoMoreInteractions(roomRepository, buildingRepository);
  }

  @Test
  void updateRoomShouldFailWhenNameAlreadyExists() {
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    // Existing room setup
    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    BuildingEntity building = new BuildingEntity();
    building.setId(buildingId);
    existingRoom.setBuilding(building);

    // Update request with conflicting name
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("ExistingRoomName"); // name already taken
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("Seats", 35)));

    // Mocks
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName("existingroomname")).thenReturn(true); // name conflict

    // Act and Assert
    GeneralProblemException exception = assertThrows(GeneralProblemException.class, () ->
      roomService.updateRoom(roomId, updateRequest)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals("Room with name %s already exists".formatted("existingroomname"), exception.getMessage());

    // Verify interactions
    verify(roomRepository).findById(roomId);
    verify(roomRepository).existsByName("existingroomname");
    verifyNoInteractions(buildingRepository);
  }
}

