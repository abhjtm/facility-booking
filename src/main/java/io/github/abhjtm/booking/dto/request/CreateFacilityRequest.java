package io.github.abhjtm.booking.dto.request;

public record CreateFacilityRequest(
    String name,
    String description,
    int apartmentId
) {
}

