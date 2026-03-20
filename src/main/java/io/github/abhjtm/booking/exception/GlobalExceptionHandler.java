package io.github.abhjtm.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidBookingException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBooking(InvalidBookingException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "BOOKING_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "BOOKING_FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        ApiErrorResponse body = new ApiErrorResponse(
            ex.getCode(),
            ex.getMessage(),
            status.value(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
