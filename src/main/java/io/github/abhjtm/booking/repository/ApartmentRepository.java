package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Apartment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Apartment persistence.
 */
public interface ApartmentRepository {
    
    Apartment save(Apartment apartment);
    
    Optional<Apartment> findById(int id);
    
    List<Apartment> findAll();
    
    void deleteById(int id);
}

