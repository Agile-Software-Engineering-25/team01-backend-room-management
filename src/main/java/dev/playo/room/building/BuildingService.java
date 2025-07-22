package dev.playo.room.building;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.Building;
import dev.playo.generated.roommanagement.model.BuildingCreateRequest;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.exception.GeneralProblemException;
import dev.playo.room.room.RoomService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BuildingService {

  private final RoomService roomService;
  private final BookingRepository bookingRepository;
  private final BuildingRepository buildingRepository;

  @Autowired
  public BuildingService(
    @NonNull RoomService roomService,
    @NonNull BookingRepository bookingRepository,
    @NonNull BuildingRepository buildingRepository) {
    this.roomService = roomService;
    this.bookingRepository = bookingRepository;
    this.buildingRepository = buildingRepository;
  }

  public @NonNull Building createBuilding(@NonNull BuildingCreateRequest request) {
    var lowerCaseName = request.getName().toLowerCase();
    if (this.buildingRepository.existsByName(lowerCaseName)) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Building with name '%s' already exists".formatted(request.getName()));
    }

    var buildingEntity = new BuildingEntity();
    buildingEntity.setName(lowerCaseName);
    buildingEntity.setDescription(request.getDescription());
    buildingEntity.setAddress(request.getAddress());
    buildingEntity.setState(request.getState());

    return this.buildingRepository.save(buildingEntity).toBuildingDto();
  }

  public @NonNull BuildingEntity findBuildingById(@NonNull UUID buildingId) {
    var building = this.buildingRepository.findById(buildingId).orElse(null);
    if (building == null) {
      throw new GeneralProblemException(
        HttpStatus.NOT_FOUND,
        "Building with ID %s does not exist".formatted(buildingId));
    }

    return building;
  }

  public @NonNull Building updateBuilding(@NonNull UUID buildingId, @NonNull BuildingCreateRequest request) {
    var building = this.findBuildingById(buildingId);
    var lowerCaseName = request.getName().toLowerCase();
    if (!building.getName().equals(lowerCaseName) && this.buildingRepository.existsByName(lowerCaseName)) {
      throw new GeneralProblemException(
        HttpStatus.BAD_REQUEST,
        "Building with name '%s' already exists".formatted(request.getName()));
    }

    building.setName(lowerCaseName);
    building.setDescription(request.getDescription());
    building.setAddress(request.getAddress());
    building.setState(request.getState());

    return this.buildingRepository.save(building).toBuildingDto();
  }

  public void deleteBuildingById(@NonNull UUID buildingId) {
    var building = this.findBuildingById(buildingId);
    this.buildingRepository.delete(building);
  }

  public @NonNull List<Building> allBuildings() {
    return this.buildingRepository.findAll().stream().map(BuildingEntity::toBuildingDto).toList();
  }

  public @NonNull List<Room> allRoomsByBuildingId(@NonNull UUID buildingId) {
    return this.roomService.findRoomsByBuildingId(buildingId);
  }

  public @NonNull List<Booking> allBookingsByBuildingIdAndDate(@NonNull UUID buildingId, @NonNull LocalDate date) {
    var building = this.findBuildingById(buildingId);
    return this.bookingRepository.findBookingByBuildingAndDate(building, date)
      .stream()
      .map(BookingEntity::toBookingDto)
      .toList();
  }
}
