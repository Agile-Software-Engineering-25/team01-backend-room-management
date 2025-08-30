package dev.playo.room.unit.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.RoomBookingRequest;
import dev.playo.room.booking.BookingService;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.config.BusinessConfiguration;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.util.Characteristics;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class BookingServiceTest {

  @Mock
  private RoomService roomService;
  @Mock
  private BookingRepository bookingRepository;
  @Mock
  private BusinessConfiguration businessConfiguration;

  @InjectMocks
  private BookingService bookingService;

  @BeforeEach
  void setUp() {
    when(this.businessConfiguration.getMultiDayBookingMaxDays()).thenReturn(0);
    when(this.businessConfiguration.getEarlyBookingTime()).thenReturn(LocalTime.of(8, 0));
    when(this.businessConfiguration.getLateBookingTime()).thenReturn(LocalTime.of(18, 0));
  }

  @Test
  @DisplayName("createBooking returns Booking on valid request")
  void createBookingReturnsBookingOnValidRequest() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));
    request.setLecturerIds(Set.of(UUID.randomUUID()));
    request.setStudentGroupIds(Set.of(UUID.randomUUID()));

    var seatsCharacteristic = new Characteristic();
    seatsCharacteristic.setType(Characteristics.SEATS_CHARACTERISTIC);
    seatsCharacteristic.setValue(10);

    var room = mock(RoomEntity.class);
    when(room.getName()).thenReturn("Raum 1");
    when(room.getCharacteristics()).thenReturn(List.of(seatsCharacteristic));
    when(roomService.findRoomById(request.getRoomId())).thenReturn(room);

    var bookingEntity = mock(BookingEntity.class);
    when(bookingRepository.save(any())).thenReturn(bookingEntity);
    var bookingDto = mock(Booking.class);
    when(bookingEntity.toBookingDto()).thenReturn(bookingDto);

    var result = bookingService.createBooking(request);

    assertNotNull(result);
    verify(bookingRepository).save(any());
  }

  @Test
  @DisplayName("createBooking throws if start time is after end time")
  void createBookingThrowsIfStartTimeAfterEndTime() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 13, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertTrue(ex.getDescription().contains("Start time must be before end time"));
  }

  @Test
  @DisplayName("createBooking throws if start time equals end time")
  void createBookingThrowsIfStartTimeEqualsEndTime() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertTrue(ex.getDescription().contains("Cannot book a room without a duration"));
  }

  @Test
  @DisplayName("createBooking throws if booking duration exceeds max days")
  void createBookingThrowsIfDurationExceedsMaxDays() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 6, 9, 0).atOffset(ZoneOffset.UTC));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertTrue(ex.getDescription().contains("Cannot book a room for more than"));
  }

  @Test
  @DisplayName("createBooking throws if booking starts before allowed time")
  void createBookingThrowsIfBookingStartsTooEarly() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 5, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertTrue(ex.getDescription().contains("Cannot book a room before"));
  }

  @Test
  @DisplayName("createBooking throws if booking ends after allowed time")
  void createBookingThrowsIfBookingEndsTooLate() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 17, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 19, 0).atOffset(ZoneOffset.UTC));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertTrue(ex.getDescription().contains("Cannot book a room after"));
  }

  @Test
  @DisplayName("createBooking throws conflict if DataIntegrityViolationException occurs")
  void createBookingThrowsConflictOnDataIntegrityViolation() {
    var request = new RoomBookingRequest();
    request.setRoomId(UUID.randomUUID());
    request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0).atOffset(ZoneOffset.UTC));
    request.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0).atOffset(ZoneOffset.UTC));
    request.setLecturerIds(Set.of(UUID.randomUUID()));
    request.setStudentGroupIds(Set.of(UUID.randomUUID()));

    var seatsCharacteristic = new Characteristic();
    seatsCharacteristic.setType(Characteristics.SEATS_CHARACTERISTIC);
    seatsCharacteristic.setValue(10);

    var room = mock(RoomEntity.class);
    when(room.getName()).thenReturn("Raum 1");
    when(room.getCharacteristics()).thenReturn(List.of(seatsCharacteristic));
    when(this.roomService.findRoomById(request.getRoomId())).thenReturn(room);

    when(this.bookingRepository.save(any())).thenThrow(new DataIntegrityViolationException("overlap"));

    var ex = assertThrows(GeneralProblemException.class, () -> bookingService.createBooking(request));
    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertTrue(ex.getDescription().contains("overlaps with an existing booking"));
  }

  @Test
  @DisplayName("findBooking returns booking if found")
  void findBookingReturnsBookingIfFound() {
    var bookingId = UUID.randomUUID();
    var entity = mock(BookingEntity.class);
    when(this.bookingRepository.findById(bookingId)).thenReturn(Optional.of(entity));

    var result = this.bookingService.findBooking(bookingId);

    assertEquals(entity, result);
  }

  @Test
  @DisplayName("findBooking throws if booking not found")
  void findBookingThrowsIfNotFound() {
    var bookingId = UUID.randomUUID();
    when(this.bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

    var ex = assertThrows(GeneralProblemException.class, () -> this.bookingService.findBooking(bookingId));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    assertTrue(ex.getDescription().contains("does not exist"));
  }

  @Test
  @DisplayName("allKnownBookings returns all bookings as DTOs")
  void allKnownBookingsReturnsAllBookings() {
    var entity1 = mock(BookingEntity.class);
    var entity2 = mock(BookingEntity.class);
    var dto1 = mock(Booking.class);
    var dto2 = mock(Booking.class);
    when(entity1.toBookingDto()).thenReturn(dto1);
    when(entity2.toBookingDto()).thenReturn(dto2);
    when(this.bookingRepository.findAll()).thenReturn(List.of(entity1, entity2));

    var result = this.bookingService.allKnownBookings();

    assertEquals(2, result.size());
    assertTrue(result.contains(dto1));
    assertTrue(result.contains(dto2));
  }

  @Test
  @DisplayName("cancelBooking deletes booking if found")
  void cancelBookingDeletesBookingIfFound() {
    var bookingId = UUID.randomUUID();
    var entity = mock(BookingEntity.class);
    when(this.bookingRepository.findById(bookingId)).thenReturn(Optional.of(entity));

    this.bookingService.cancelBooking(bookingId);

    verify(this.bookingRepository).delete(entity);
  }
}
