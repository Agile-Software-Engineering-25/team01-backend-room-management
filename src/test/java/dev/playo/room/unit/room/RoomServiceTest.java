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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

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
  void createRoomWithDuplicateNameShouldThrowGeneralProblemException() {
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());

    when(roomRepository.existsByName(request.getName().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.createRoom(request));

    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void createRoomWithDuplicateChemSymbolShouldThrowGeneralProblemException() {
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");

    request.setBuildingId(UUID.randomUUID());
    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.createRoom(request));

    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void updateRoomWithDuplicateChemSymbolShouldThrowGeneralProblemException() {
    UUID roomId = UUID.randomUUID();

    // Existing room setup
    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("old room");
    existingRoom.setChemSymbol("old chemSymbol");

    // Update request with duplicate ChemSymbol
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 30)));

    // Mocking
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(true);

    // Assertion
    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, request));

    // Verification
    verify(roomRepository, times(1)).findById(roomId);
    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, times(1)).existsByChemSymbol(request.getChemSymbol().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void updateRoomShouldChangeName() {
    // Setup
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    List<Characteristic> characteristics = List.of(
      new Characteristic("SEATS", 1),
      new Characteristic("Projector", 1)
    );

    BuildingEntity buildingEntity = new BuildingEntity();
    buildingEntity.setId(buildingId);

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    existingRoom.setChemSymbol("Hydrogenium");
    existingRoom.setBuilding(buildingEntity);
    existingRoom.setCharacteristics(characteristics);

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("newroomname");
    updateRequest.setChemSymbol("Hydrogenium");
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
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    BuildingEntity buildingEntity = new BuildingEntity();
    buildingEntity.setId(buildingId);

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    existingRoom.setChemSymbol("Hydrogenium");
    existingRoom.setBuilding(buildingEntity);
    existingRoom.setCharacteristics(List.of(new Characteristic("SEATS", 20)));

    List<Characteristic> newCharacteristics = List.of(
      new Characteristic("SEATS", 1),
      new Characteristic("Projector", 1)
    );

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setChemSymbol("Hydrogenium");
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
    existingRoom.setChemSymbol("Hydrogenium");
    BuildingEntity oldBuilding = new BuildingEntity();
    oldBuilding.setId(oldBuildingId);
    existingRoom.setBuilding(oldBuilding);

    // Update request with new building ID
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setChemSymbol("Hydrogenium");
    updateRequest.setBuildingId(newBuildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("SEATS", 30)));

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
  }

  @Test
  void updateRoomShouldThrowExceptionWhenRoomNotFound() {
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("NewRoomName");
    updateRequest.setChemSymbol("Hydrogenium");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("SEATS", 20)));

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
    existingRoom.setChemSymbol("Hydrogenium");
    BuildingEntity building = new BuildingEntity();
    building.setId(buildingId);
    existingRoom.setBuilding(building);

    // Update request with conflicting name
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("ExistingRoomName"); // name already taken
    updateRequest.setChemSymbol("Hydrogenium");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(new Characteristic("SEATS", 35)));

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

  @Test
  void updateRoomShouldFailWhenNewRoomIsWithoutSeats() {
    UUID roomId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    // Existing room setup
    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("oldname");
    existingRoom.setChemSymbol("Hydrogenium");
    BuildingEntity building = new BuildingEntity();
    building.setId(buildingId);
    existingRoom.setBuilding(building);

    // Update request without "Seats" characteristic
    RoomCreateRequest updateRequest = new RoomCreateRequest();
    updateRequest.setName("newroomname");
    updateRequest.setChemSymbol("Hydrogenium");
    updateRequest.setBuildingId(buildingId);
    updateRequest.setCharacteristics(List.of(
      new Characteristic("Projector", 1),
      new Characteristic("Whiteboard", 1)
    ));

    // Mocks
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));

    // Act and Assert
    GeneralProblemException exception = assertThrows(GeneralProblemException.class, () ->
      roomService.updateRoom(roomId, updateRequest)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals("Rooms need to have at least one SEAT", exception.getMessage());

    // Verify interactions
    verify(roomRepository).findById(roomId);
    verifyNoInteractions(buildingRepository);
    verify(roomRepository, never()).existsByName(anyString());
  }

  @Test
  void createRoom_withDuplicateName_shouldThrowGeneralProblemException() {
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    when(roomRepository.existsByName(request.getName().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.createRoom(request));

    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void createRoom_withDuplicateChemSymbol_shouldThrowGeneralProblemException() {
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.createRoom(request));

    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void createRoom_withNonExistingBuildingId_shouldThrowGeneralProblemException() {
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    when(roomRepository.existsByName(request.getName().toLowerCase())).thenReturn(false);
    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(false);
    when(buildingRepository.existsById(request.getBuildingId())).thenReturn(false);

    assertThrows(GeneralProblemException.class, () -> roomService.createRoom(request));

    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, times(1)).existsByChemSymbol(request.getChemSymbol().toLowerCase());
    verify(buildingRepository, times(1)).existsById(request.getBuildingId());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void updateRoom_withDuplicateName_shouldThrowGeneralProblemException() {
    UUID roomId = UUID.randomUUID();
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("old name");
    existingRoom.setChemSymbol("old chemSymbol");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName(request.getName().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, request));

    verify(roomRepository, times(1)).findById(roomId);
    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void updateRoom_withDuplicateChemSymbol_shouldThrowGeneralProblemException() {
    UUID roomId = UUID.randomUUID();
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("old name");
    existingRoom.setChemSymbol("old chemSymbol");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(true);

    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, request));

    verify(roomRepository, times(1)).findById(roomId);
    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, times(1)).existsByChemSymbol(request.getChemSymbol().toLowerCase());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }

  @Test
  void updateRoom_withNonExistingBuildingId_shouldThrowGeneralProblemException() {
    UUID roomId = UUID.randomUUID();
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());
    request.setCharacteristics(List.of(new Characteristic("SEATS", 1)));

    RoomEntity existingRoom = new RoomEntity();
    existingRoom.setId(roomId);
    existingRoom.setName("old name");
    existingRoom.setChemSymbol("old chemSymbol");

    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
    when(roomRepository.existsByName(request.getName().toLowerCase())).thenReturn(false);
    when(roomRepository.existsByChemSymbol(request.getChemSymbol().toLowerCase())).thenReturn(false);
    when(buildingRepository.existsById(request.getBuildingId())).thenReturn(false);

    assertThrows(GeneralProblemException.class, () -> roomService.updateRoom(roomId, request));

    verify(roomRepository, times(1)).findById(roomId);
    verify(roomRepository, times(1)).existsByName(request.getName().toLowerCase());
    verify(roomRepository, times(1)).existsByChemSymbol(request.getChemSymbol().toLowerCase());
    verify(buildingRepository, times(1)).existsById(request.getBuildingId());
    verify(roomRepository, never()).save(any(RoomEntity.class));
  }
}


