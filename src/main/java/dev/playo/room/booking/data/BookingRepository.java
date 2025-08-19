package dev.playo.room.booking.data;

import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.room.data.RoomEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

  @Query("""
    SELECT booking FROM BookingEntity booking WHERE booking.room = :roomEntity
         AND :date BETWEEN CAST(booking.startTime as date) AND CAST(booking.endTime as date)
    """)
  List<BookingEntity> findBookingByRoomAndDate(@NonNull RoomEntity roomEntity, @NonNull LocalDate date);

  @Query("""
    SELECT booking FROM BookingEntity booking WHERE booking.room.building = :buildingEntity
         AND :date BETWEEN CAST(booking.startTime as date) AND CAST(booking.endTime as date)
    """)
  List<BookingEntity> findBookingByBuildingAndDate(@NonNull BuildingEntity buildingEntity, @NonNull LocalDate date);
}
