package io.github.abhjtm.booking.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
    UUID facilityId,
    String description,
    String bookedBy,
    List<String> requestedAttendees,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {
}
