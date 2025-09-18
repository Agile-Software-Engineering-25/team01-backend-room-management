package dev.playo.room.booking.data.allocation;

import dev.playo.room.booking.data.BookingEntity;
import dev.playo.room.room.data.RoomEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString
@Table(name = "booking_allocations")
public class BookingAllocation {

  @EmbeddedId
  private BookingAllocationId id;

  @MapsId("bookingId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private BookingEntity booking;

  @MapsId("roomId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private RoomEntity room;

  @Column(nullable = false)
  private Instant startTime;

  @Column(nullable = false)
  private Instant endTime;

}
