package dev.playo.room.booking.data.allocation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Embeddable
public class BookingAllocationId {

  @Column(name = "booking_id")
  private UUID bookingId;

  @Column(name = "room_id")
  private UUID roomId;
}
