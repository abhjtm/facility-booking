package io.github.abhjtm.booking.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateBookingRequest(
    int facilityId,
    String description,
    String bookedBy,
    List<String> requestedAttendees,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {
}
