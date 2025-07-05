package dev.playo.room.room.data;

import dev.playo.room.characteristic.CharacteristicEntity;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  private String name;

  private String locatedAt;

  @ElementCollection
  private List<CharacteristicEntity> characteristics;
}
