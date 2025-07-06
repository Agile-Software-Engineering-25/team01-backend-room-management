package dev.playo.room.room.data;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  boolean existsByName(String name);
}
