package dev.playo.room.room;

import dev.playo.generated.roommanagement.api.RoomsApi;
import dev.playo.generated.roommanagement.model.GetAllBookingsResponse;
import dev.playo.generated.roommanagement.model.GetAllRoomsResponse;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.generated.roommanagement.model.RoomCreateRequest;
import dev.playo.generated.roommanagement.model.RoomInquiry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Controller
public class RoomController implements RoomsApi {

  private final RoomService roomService;

  @Autowired
  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @Override
  public ResponseEntity<Room> createRoom(RoomCreateRequest roomCreateRequest) {
    return ResponseEntity.ok(this.roomService.createRoom(roomCreateRequest));
  }

  @Override
  public ResponseEntity<Void> deleteRoomById(UUID roomId, Boolean forceDelete) {
    this.roomService.deleteRoomById(roomId, forceDelete);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<List<Room>> findAvailableRooms(RoomInquiry roomInquiry) {
    return ResponseEntity.ok(this.roomService.findAvailableRooms(roomInquiry));
  }

  @Override
  public ResponseEntity<GetAllBookingsResponse> getBookingsForRoom(UUID roomId, LocalDate date) {
    var bookings = this.roomService.findBookingsByRoomAndDate(roomId, date);
    return ResponseEntity.ok(new GetAllBookingsResponse(bookings));
  }

  @Override
  public ResponseEntity<Room> getRoomById(UUID roomId) {
    var roomEntity = this.roomService.findRoomById(roomId);
    return ResponseEntity.ok(roomEntity.toRoomDto());
  }

  @Override
  public ResponseEntity<GetAllRoomsResponse> getRooms() {
    return ResponseEntity.ok(new GetAllRoomsResponse(this.roomService.allKnownRooms()));
  }

  @Override
  public ResponseEntity<Room> updateRoomById(UUID roomId, RoomCreateRequest roomCreateRequest) {
    return ResponseEntity.ok(this.roomService.updateRoom(roomId, roomCreateRequest));
  }
}
