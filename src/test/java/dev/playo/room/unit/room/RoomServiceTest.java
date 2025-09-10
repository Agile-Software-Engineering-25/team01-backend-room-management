package dev.playo.room.unit.room;

import dev.playo.generated.roommanagement.model.RoomInquiry;
import dev.playo.generated.roommanagement.model.SearchCharacteristic;
import dev.playo.room.booking.data.BookingRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
  void findAvailableRoomsShouldThrowGeneralProblemException() {
    Map<String, Object> complexValue = Map.of("foo", "bar");
    SearchCharacteristic invalidSearch =
      new SearchCharacteristic("Test", complexValue, SearchCharacteristic.OperatorEnum.EQUALS);

    RoomInquiry roomInquiry = new RoomInquiry(
      OffsetDateTime.now(),
      OffsetDateTime.now().plusHours(2),
      UUID.randomUUID(),
      List.of(invalidSearch)
    );

    GeneralProblemException exception =
    assertThrows(GeneralProblemException.class, () -> roomService.findAvailableRooms(roomInquiry));

    assertEquals("Complex objects are not supported as characteristic values.",  exception.getMessage());
  }
}
