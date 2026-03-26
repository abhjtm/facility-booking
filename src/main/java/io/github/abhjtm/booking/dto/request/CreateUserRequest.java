package io.github.abhjtm.booking.dto.request;

public record CreateUserRequest(
    String name,
    String email,
    String flatNumber,
    int apartmentId
) {
}

