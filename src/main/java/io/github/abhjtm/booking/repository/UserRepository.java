package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User persistence.
 */
public interface UserRepository {
    
    User save(User user);
    
    Optional<User> findById(int id);
    
    Optional<User> findByEmail(String email);
    
    List<User> findAll();
    
    List<User> findByApartmentId(int apartmentId);
    
    void deleteById(int id);
}



