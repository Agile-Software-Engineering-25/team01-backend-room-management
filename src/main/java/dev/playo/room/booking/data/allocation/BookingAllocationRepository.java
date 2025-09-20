package dev.playo.room.booking.data.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingAllocationRepository extends JpaRepository<BookingAllocation, BookingAllocationId> {

}
