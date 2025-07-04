package dev.playo.room.booking.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table
public class BookingEntity {

  @Id
  private UUID id;

}
