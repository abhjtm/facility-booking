package io.github.abhjtm.booking.dto.request;

import java.util.List;

public record UpdateBookingAttendeesRequest(
    String bookedBy,
    List<String> attendeesToAdd,
    List<String> attendeesToRemove
) {
}
