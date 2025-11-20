package dev.playo.room.room.data;

import dev.playo.generated.roommanagement.model.Characteristic;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.room.building.data.BuildingEntity;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "rooms")
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_room_id")
  private RoomEntity parent;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
  private Set<RoomEntity> composedOf = new HashSet<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> defects = new ArrayList<>();

  public @NonNull Room toRoomDto() {
    return new Room()
      .id(this.getId())
      .name(this.getName())
      .chemSymbol(this.getChemSymbol())
      .buildingId(this.getBuilding().getId())
      .characteristics(this.getCharacteristics())
      .composedOf(this.composedOf.stream().map(RoomEntity::toRoomDto).toList())
      .defects(this.getDefects());
  }
}
