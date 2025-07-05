package dev.playo.room.room;

import dev.playo.generated.roommanagement.api.RoomsApi;
import dev.playo.generated.roommanagement.model.GetAllBookingsResponse;
import dev.playo.generated.roommanagement.model.GetAllRoomsResponse;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.generated.roommanagement.model.RoomInquiry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class RoomController implements RoomsApi {

  @Override
  public ResponseEntity<Room> createRoom(Room room) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteRoomById(UUID roomId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Room>> findAvailableRooms(RoomInquiry roomInquiry) {
    return null;
  }

  @Override
  public ResponseEntity<GetAllBookingsResponse> getBookingsForRoom(UUID roomId, LocalDate date) {
    return null;
  }

  @Override
  public ResponseEntity<Room> getRoomById(UUID roomId) {
    return null;
  }

  @Override
  public ResponseEntity<GetAllRoomsResponse> getRooms() {
    return null;
  }

  @Override
  public ResponseEntity<Room> updateRoomById(UUID roomId, Room room) {
    return null;
  }
}
