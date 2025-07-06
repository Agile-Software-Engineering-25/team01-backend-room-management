package dev.playo.room.room.data;

import dev.playo.generated.roommanagement.model.Room;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  private String name;

  private String locatedAt;

  public @NonNull Room toRoomDto() {
    return new Room()
      .id(this.getId())
      .name(this.getName())
      .locatedAt(this.getLocatedAt());
  }
}
