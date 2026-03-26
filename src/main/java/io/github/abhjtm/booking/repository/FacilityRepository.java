package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Facility;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Facility persistence.
 */
public interface FacilityRepository {
    
    Facility save(Facility facility);
    
    Optional<Facility> findById(int id);
    
    List<Facility> findAll();
    
    List<Facility> findByApartmentId(int apartmentId);
    
    void deleteById(int id);
}

