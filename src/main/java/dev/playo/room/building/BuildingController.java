package dev.playo.room.building;

import dev.playo.generated.roommanagement.api.BuildingsApi;
import dev.playo.generated.roommanagement.model.Building;
import dev.playo.generated.roommanagement.model.BuildingCreateRequest;
import dev.playo.generated.roommanagement.model.GetAllBookingsResponse;
import dev.playo.generated.roommanagement.model.GetAllBuildingsResponse;
import dev.playo.generated.roommanagement.model.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class BuildingController implements BuildingsApi {

  private final BuildingService buildingService;

  @Autowired
  public BuildingController(@NonNull BuildingService buildingService) {
    this.buildingService = buildingService;
  }

  @Override
  public ResponseEntity<Building> createBuilding(BuildingCreateRequest buildingCreateRequest) {
    return ResponseEntity.ok(this.buildingService.createBuilding(buildingCreateRequest));
  }

  @Override
  public ResponseEntity<Void> deleteBuilding(UUID buildingId) {
    this.buildingService.deleteBuildingById(buildingId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<GetAllBookingsResponse> getBookingsForBuilding(UUID buildingId, LocalDate date) {
    var bookings = this.buildingService.allBookingsByBuildingIdAndDate(buildingId, date);
    return ResponseEntity.ok(new GetAllBookingsResponse(bookings));
  }

  @Override
  public ResponseEntity<Building> getBuildingById(UUID buildingId) {
    var building = this.buildingService.findBuildingById(buildingId);
    return ResponseEntity.ok(building.toBuildingDto());
  }

  @Override
  public ResponseEntity<GetAllBuildingsResponse> getBuildings() {
    var buildings = this.buildingService.allBuildings();
    return ResponseEntity.ok(new GetAllBuildingsResponse(buildings));
  }

  @Override
  public ResponseEntity<List<Room>> getRoomsForBuilding(UUID buildingId) {
    var rooms = this.buildingService.allRoomsByBuildingId(buildingId);
    return ResponseEntity.ok(rooms);
  }

  @Override
  public ResponseEntity<Building> updateBuilding(UUID buildingId, BuildingCreateRequest buildingCreateRequest) {
    var update = this.buildingService.updateBuilding(buildingId, buildingCreateRequest);
    return ResponseEntity.ok(update);
  }
}
