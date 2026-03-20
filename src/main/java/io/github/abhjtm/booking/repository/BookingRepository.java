package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Booking persistence.
 * Can be implemented by file storage, database, or any other storage mechanism.
 */
public interface BookingRepository {
    
    Booking save(Booking booking);
    
    Optional<Booking> findById(UUID id);
    
    List<Booking> findAll();
    
    List<Booking> findByFacilityId(UUID facilityId);
    
    void deleteById(UUID id);
}
