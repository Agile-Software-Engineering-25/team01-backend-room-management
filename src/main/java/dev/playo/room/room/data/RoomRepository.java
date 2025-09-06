package dev.playo.room.room.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  boolean existsByName(String name);

  boolean existsByChemSymbol(String name);

  List<RoomEntity> findRoomEntityByBuildingId(UUID buildingId);
}

