package dev.playo.room.room.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  boolean existsByName(String name);

  boolean existsByChemSymbol(String chemSymbol);

  @Modifying
  @Query("UPDATE RoomEntity r SET r.parent = NULL WHERE r.parent.id = :parentId")
  void unsetParentForAllChildren(UUID parentId);

  List<RoomEntity> findRoomEntityByBuildingId(UUID buildingId);

  @Query(value = "SELECT * FROM rooms WHERE rooms.parent_room_id IS NULL AND NOT EXISTS(SELECT 1 FROM rooms as child WHERE child.parent_room_id = rooms.id)", nativeQuery = true)
  List<RoomEntity> findRoomsEligibleForComposing();
}

