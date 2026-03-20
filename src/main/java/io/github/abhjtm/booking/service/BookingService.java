package io.github.abhjtm.booking.service;

import io.github.abhjtm.booking.dto.BookingStatus;
import io.github.abhjtm.booking.dto.request.CreateBookingRequest;
import io.github.abhjtm.booking.dto.request.UpdateBookingAttendeesRequest;
import io.github.abhjtm.booking.dto.response.Booking;
import io.github.abhjtm.booking.exception.InvalidBookingException;
import io.github.abhjtm.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private static final int MAX_ADVANCE_BOOKING_DAYS = 7;
    
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking createBooking(CreateBookingRequest request) {
        validateTimeSlot(request);
        checkAvailability(request);
        
        // TODO: Validate facility exists
        List<String> confirmedAttendees = new ArrayList<>();
        confirmedAttendees.add(request.bookedBy());

        Booking booking = new Booking(
            UUID.randomUUID(),
            request.facilityId(),
            request.description(),
            request.bookedBy(),
            request.requestedAttendees(),
            confirmedAttendees,
            request.startTime(),
            request.endTime(),
            BookingStatus.PENDING,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        
        return bookingRepository.save(booking);
    }

    public Optional<Booking> getBookingById(UUID id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByFacility(UUID facilityId) {
        return bookingRepository.findByFacilityId(facilityId);
    }

    public Booking cancelBooking(UUID bookingId, String bookedBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingException("BOOKING_NOT_FOUND", "Booking not found"));

        if (bookedBy == null || bookedBy.isBlank()) {
            throw new InvalidBookingException("BOOKED_BY_REQUIRED", "bookedBy is required");
        }

        if (!booking.bookedBy().equals(bookedBy)) {
            throw new InvalidBookingException("BOOKING_FORBIDDEN", "Only bookedBy can cancel this booking");
        }

        if (booking.status() == BookingStatus.CANCELLED || booking.status() == BookingStatus.COMPLETED) {
            throw new InvalidBookingException("BOOKING_NOT_ACTIVE", "Only active bookings can be cancelled");
        }

        Booking cancelled = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedBy(),
                booking.requestedAttendees(),
                booking.confirmedAttendees(),
                booking.startTime(),
                booking.endTime(),
                BookingStatus.CANCELLED,
                booking.createdAt(),
                OffsetDateTime.now()
        );

        return bookingRepository.save(cancelled);
    }

    public Booking updateBookingAttendees(UUID bookingId, UpdateBookingAttendeesRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingException("BOOKING_NOT_FOUND", "Booking not found"));

        if (request.bookedBy() == null || request.bookedBy().isBlank()) {
            throw new InvalidBookingException("BOOKED_BY_REQUIRED", "bookedBy is required");
        }

        if (!booking.bookedBy().equals(request.bookedBy())) {
            throw new InvalidBookingException("BOOKING_FORBIDDEN", "Only bookedBy can update attendees");
        }

        if (booking.status() == BookingStatus.CANCELLED || booking.status() == BookingStatus.COMPLETED) {
            throw new InvalidBookingException("BOOKING_NOT_ACTIVE", "Only active bookings can be updated");
        }

        List<String> attendeesToAdd = request.attendeesToAdd() == null
                ? List.of()
                : request.attendeesToAdd();
        List<String> attendeesToRemove = request.attendeesToRemove() == null
                ? List.of()
                : request.attendeesToRemove();

        if (attendeesToAdd.isEmpty() && attendeesToRemove.isEmpty()) {
            throw new InvalidBookingException(
                    "INVALID_UPDATE_ACTION",
                    "Provide attendeesToAdd and/or attendeesToRemove"
            );
        }

        List<String> requestedAttendees = new ArrayList<>(booking.requestedAttendees());
        attendeesToAdd.stream()
                .filter(email -> !requestedAttendees.contains(email))
                .forEach(requestedAttendees::add);
        requestedAttendees.removeIf(attendeesToRemove::contains);

        List<String> confirmedAttendees = new ArrayList<>(booking.confirmedAttendees());
        confirmedAttendees.removeIf(attendeesToRemove::contains);

        Booking updated = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedBy(),
                requestedAttendees,
                confirmedAttendees,
                booking.startTime(),
                booking.endTime(),
                booking.status(),
                booking.createdAt(),
                OffsetDateTime.now()
        );

        return bookingRepository.save(updated);
    }

    public Booking confirmAttendance(UUID bookingId, String attendeeEmail) {
        if (attendeeEmail == null || attendeeEmail.isBlank()) {
            throw new InvalidBookingException("ATTENDEE_EMAIL_REQUIRED", "attendeeEmail is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingException("BOOKING_NOT_FOUND", "Booking not found"));

        if (booking.status() == BookingStatus.CANCELLED || booking.status() == BookingStatus.COMPLETED) {
            throw new InvalidBookingException("BOOKING_NOT_ACTIVE", "Cannot confirm attendance for inactive booking");
        }

        if (!booking.requestedAttendees().contains(attendeeEmail)) {
            throw new InvalidBookingException(
                    "ATTENDEE_NOT_REQUESTED",
                    "Only requested attendees can confirm attendance"
            );
        }

        List<String> confirmedAttendees = new ArrayList<>(booking.confirmedAttendees());
        if (!confirmedAttendees.contains(attendeeEmail)) {
            confirmedAttendees.add(attendeeEmail);
        }

        Booking updated = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedBy(),
                booking.requestedAttendees(),
                confirmedAttendees,
                booking.startTime(),
                booking.endTime(),
                BookingStatus.CONFIRMED,
                booking.createdAt(),
                OffsetDateTime.now()
        );

        return bookingRepository.save(updated);
    }

    private void checkAvailability(CreateBookingRequest request) {
        boolean overlaps = bookingRepository.findByFacilityId(request.facilityId()).stream()
                .filter(b -> b.status() != BookingStatus.CANCELLED && b.status() != BookingStatus.COMPLETED)
                .anyMatch(b -> request.startTime().isBefore(b.endTime()) && request.endTime().isAfter(b.startTime()));

        if (overlaps) {
            throw new InvalidBookingException("TIME_SLOT_UNAVAILABLE", "The requested time slot overlaps with an existing booking");
        }
    }

    private void validateTimeSlot(CreateBookingRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime maxAllowedDate = now.plusDays(MAX_ADVANCE_BOOKING_DAYS);

        // Check if start time is in the past
        if (request.startTime().isBefore(now)) {
            throw new InvalidBookingException("Booking start time cannot be in the past");
        }

        // Check if end time is before start time
        if (request.endTime().isBefore(request.startTime())) {
            throw new InvalidBookingException("Booking end time must be after start time");
        }

        // Check if booking is within next 7 days
        if (request.startTime().isAfter(maxAllowedDate)) {
            throw new InvalidBookingException(
                "Bookings can only be made up to " + MAX_ADVANCE_BOOKING_DAYS + " days in advance"
            );
        }

        // Check if booking duration is less than 1 hour
        if (Duration.between(request.startTime(), request.endTime()).toMinutes() >= 60) {
            throw new InvalidBookingException("Booking duration must be less than 1 hour");
        }
    }
}
