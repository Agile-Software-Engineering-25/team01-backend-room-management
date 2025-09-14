package dev.playo.room.room.data;

import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  private String name;

  @Column(name = "chem_symbol")
  private String chemSymbol;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private BuildingEntity building;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<Characteristic> characteristics;

  public @NonNull Room toRoomDto() {
    return new Room()
      .id(this.getId())
      .name(this.getName())
      .chemSymbol(this.getChemSymbol())
      .buildingId(this.building.getId())
      .characteristics(this.getCharacteristics());
  }
}
