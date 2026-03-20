package io.github.abhjtm.booking.exception;

public class InvalidBookingException extends RuntimeException {

    private final String code;

    public InvalidBookingException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public InvalidBookingException(String message) {
        this("INVALID_BOOKING", message);
    }

    public String getCode() {
        return code;
    }
}
