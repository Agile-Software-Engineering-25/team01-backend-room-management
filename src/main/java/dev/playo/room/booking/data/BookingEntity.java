package dev.playo.room.booking.data;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.util.DateTimeNormalizer;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@ToString
@Table(name = "bookings")
public class BookingEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  @Column(nullable = false)
  private Instant startTime;

  @Column(nullable = false)
  private Instant endTime;

  @Column(nullable = false)
  private Set<UUID> lecturerIds = new HashSet<>();

  @Column(nullable = false)
  private Set<UUID> studentGroupIds = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private RoomEntity room;

  public @NonNull Booking toBookingDto() {
    return new Booking()
      .id(this.id)
      .roomId(this.room.getId())
      .startTime(DateTimeNormalizer.fromInstant(this.startTime))
      .endTime(DateTimeNormalizer.fromInstant(this.endTime))
      .lecturerIds(this.lecturerIds)
      .studentGroupIds(this.studentGroupIds);
  }
}
