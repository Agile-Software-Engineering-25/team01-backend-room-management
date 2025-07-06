package dev.playo.room.booking.data;

import dev.playo.generated.roommanagement.model.Booking;
import dev.playo.generated.roommanagement.model.Room;
import dev.playo.room.room.data.RoomEntity;
import dev.playo.room.util.UUID7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@Table(name = "bookings")
public class BookingEntity {

  @Id
  @UuidGenerator(algorithm = UUID7Generator.class)
  private UUID id;

  @Column(nullable = false)
  private Instant startTime;

  @Column(nullable = false)
  private Instant endTime;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private RoomEntity room;

  public @NonNull Booking toBookingDto() {
    return new Booking()
        .id(this.id)
        .startTime(OffsetDateTime.ofInstant(this.startTime, ZoneOffset.systemDefault()))
        .endTime(OffsetDateTime.ofInstant(this.endTime, ZoneOffset.systemDefault()))
        .roomId(this.room.getId());
  }
}
