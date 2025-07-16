package dev.playo.room.building.data;

import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<BuildingEntity, UUID> {

  boolean existsByName(@NonNull String name);

}
