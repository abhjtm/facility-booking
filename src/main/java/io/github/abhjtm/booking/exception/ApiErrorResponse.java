package io.github.abhjtm.booking.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    String code,
    String message,
    int status,
    OffsetDateTime timestamp
) {
}
