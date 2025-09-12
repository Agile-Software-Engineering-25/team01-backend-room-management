package dev.playo.room.building.data;

import dev.playo.generated.roommanagement.model.Building;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@Table(name = "buildings")
public class BuildingEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  private String name;

  private String description;

  private String address;

  public Building toBuildingDto() {
    return new Building()
      .id(this.id)
      .name(this.name)
      .address(this.address)
      .description(this.description);
  }
}
