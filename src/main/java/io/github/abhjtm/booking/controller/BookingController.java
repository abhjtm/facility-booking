package io.github.abhjtm.booking.controller;

import io.github.abhjtm.booking.dto.request.ConfirmAttendanceRequest;
import io.github.abhjtm.booking.dto.request.CancelBookingRequest;
import io.github.abhjtm.booking.dto.request.CreateBookingRequest;
import io.github.abhjtm.booking.dto.request.UpdateBookingAttendeesRequest;
import io.github.abhjtm.booking.dto.response.Booking;
import io.github.abhjtm.booking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /**
     * Get all bookings made by a specific user
     * @param userEmail The email of the user who made the booking
     * @return List of bookings made by the user, sorted by start time (newest first)
     */
    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<Booking>> getBookingsByUser(@PathVariable String userEmail) {
        List<Booking> bookings = bookingService.getBookingsByUser(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable java.util.UUID bookingId,
            @RequestBody CancelBookingRequest request) {
        Booking booking = bookingService.cancelBooking(bookingId, request.bookedBy());
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{bookingId}/attendees")
    public ResponseEntity<Booking> updateBookingAttendees(
            @PathVariable java.util.UUID bookingId,
            @RequestBody UpdateBookingAttendeesRequest request) {
        Booking booking = bookingService.updateBookingAttendees(bookingId, request);
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{bookingId}/confirm-attendance")
    public ResponseEntity<Booking> confirmAttendance(
            @PathVariable java.util.UUID bookingId,
            @RequestBody ConfirmAttendanceRequest request) {
        Booking booking = bookingService.confirmAttendance(bookingId, request.attendeeEmail());
        return ResponseEntity.ok(booking);
    }
}
