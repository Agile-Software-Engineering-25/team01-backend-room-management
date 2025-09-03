package dev.playo.room.room;

import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.room.data.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
  void createRoom_withDuplicateName_shouldThrowGeneralProblemException() {
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
  void createRoom_withDuplicateChemSymbol_shouldThrowGeneralProblemException() {
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
  void updateRoom_withDuplicateName_shouldThrowGeneralProblemException() {
    UUID roomId = UUID.randomUUID();
    RoomCreateRequest request = new RoomCreateRequest();
    request.setName("Conference Room");
    request.setChemSymbol("Hydrogenium");
    request.setBuildingId(UUID.randomUUID());

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
}
