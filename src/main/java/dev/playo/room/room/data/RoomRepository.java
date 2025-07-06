package dev.playo.room.room.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  boolean existsByName(String name);

  @Query("""
          SELECT r FROM RoomEntity r WHERE r.id NOT IN
          (SELECT b.room.id FROM BookingEntity b WHERE(b.startTime < :endTime AND b.endTime > :startTime))
      """)
    // TODO: capacity
  List<RoomEntity> findAvailableRooms(@NonNull Instant startTime, @NonNull Instant endTime, int groupSize);
}
