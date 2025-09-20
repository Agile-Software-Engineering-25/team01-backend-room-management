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
}

