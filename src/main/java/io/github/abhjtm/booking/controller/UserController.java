package io.github.abhjtm.booking.controller;

import io.github.abhjtm.booking.dto.request.CreateUserRequest;
import io.github.abhjtm.booking.dto.response.User;
import io.github.abhjtm.booking.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user
     * @param request CreateUserRequest with name, email, flatNumber, and apartmentId (must exist in apartments table)
     * @return Created user with assigned ID
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Get users by apartment ID
     * @param apartmentId The apartment ID to filter users
     * @return List of users in the specified apartment
     */
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<User>> getUsersByApartment(@PathVariable int apartmentId) {
        List<User> users = userService.getUsersByApartment(apartmentId);
        return ResponseEntity.ok(users);
    }

    /**
     * Delete a user
     * @param userId The user ID to delete
     * @param apartmentId The apartment ID (for verification that user belongs to this apartment)
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/{userId}/apartment/{apartmentId}")
    public ResponseEntity<Void> deleteUser(@PathVariable int userId, @PathVariable int apartmentId) {
        userService.deleteUser(userId, apartmentId);
        return ResponseEntity.noContent().build();
    }
}

