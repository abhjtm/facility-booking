package io.github.abhjtm.booking.dto.response;

public record Facility(
    int id,
    String name,
    String description,
    int apartmentId
) {
}

