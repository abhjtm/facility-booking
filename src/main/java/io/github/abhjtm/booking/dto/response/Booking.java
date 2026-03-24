package io.github.abhjtm.booking.dto.response;

import io.github.abhjtm.booking.dto.BookingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Booking(
    UUID id,
    int facilityId,
    String description,
    String bookedBy,
    List<String> requestedAttendees,
    List<String> confirmedAttendees,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    BookingStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
