package dev.playo.room.booking.data;

import dev.playo.generated.roommanagement.model.Booking;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {



}
