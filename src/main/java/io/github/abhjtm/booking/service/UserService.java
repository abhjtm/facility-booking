package io.github.abhjtm.booking.service;

import io.github.abhjtm.booking.dto.request.CreateUserRequest;
import io.github.abhjtm.booking.dto.response.User;
import io.github.abhjtm.booking.exception.InvalidBookingException;
import io.github.abhjtm.booking.repository.ApartmentRepository;
import io.github.abhjtm.booking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;

    public UserService(UserRepository userRepository, ApartmentRepository apartmentRepository) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
    }

    public User createUser(CreateUserRequest request) {
        validateUserRequest(request);

        // Validate apartment exists
        if (apartmentRepository.findById(request.apartmentId()).isEmpty()) {
            throw new InvalidBookingException(
                    "APARTMENT_NOT_FOUND",
                    "Apartment with id " + request.apartmentId() + " does not exist"
            );
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new InvalidBookingException("USER_EMAIL_EXISTS", "User with this email already exists");
        }

        User user = new User(
            0,
            request.name(),
            request.email(),
            request.flatNumber(),
            request.apartmentId()
        );

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByApartment(int apartmentId) {
        return userRepository.findByApartmentId(apartmentId);
    }

    public Optional<User> getUserById(int id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void validateUserExists(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new InvalidBookingException(
                    "USER_NOT_FOUND",
                    "User with email '" + email + "' does not exist"
            );
        }
    }

    public void deleteUser(int userId, int apartmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidBookingException(
                        "USER_NOT_FOUND",
                        "User with id " + userId + " does not exist"
                ));

        // Verify user belongs to the specified apartment
        if (user.apartmentId() != apartmentId) {
            throw new InvalidBookingException(
                    "APARTMENT_MISMATCH",
                    "User does not belong to apartment with id " + apartmentId
            );
        }

        userRepository.deleteById(userId);
    }

    private void validateUserRequest(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidBookingException("USER_NAME_REQUIRED", "User name is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new InvalidBookingException("USER_EMAIL_REQUIRED", "User email is required");
        }
        if (request.flatNumber() == null || request.flatNumber().isBlank()) {
            throw new InvalidBookingException("USER_FLAT_NUMBER_REQUIRED", "Flat number is required");
        }
        if (request.apartmentId() <= 0) {
            throw new InvalidBookingException("USER_APARTMENT_ID_REQUIRED", "Valid apartment ID is required");
        }
    }
}

