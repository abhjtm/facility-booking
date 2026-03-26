package io.github.abhjtm.booking.dto.response;

public record User(
    int id,
    String name,
    String email,
    String flatNumber,
    int apartmentId
) {
}


