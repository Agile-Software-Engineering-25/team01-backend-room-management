package dev.playo.room.integration;

import dev.playo.room.booking.data.BookingRepository;
import dev.playo.room.building.data.BuildingRepository;
import dev.playo.room.room.data.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestCleaner {

  private final RoomRepository roomRepository;
  private final BookingRepository bookingRepository;
  private final BuildingRepository buildingRepository;

  @Autowired
  public TestCleaner(
    RoomRepository roomRepository,
    BookingRepository bookingRepository,
    BuildingRepository buildingRepository
  ) {
    this.roomRepository = roomRepository;
    this.bookingRepository = bookingRepository;
    this.buildingRepository = buildingRepository;
  }

  public void clean() {
    this.bookingRepository.deleteAll();
    this.roomRepository.deleteAll();
    this.buildingRepository.deleteAll();
  }
}
