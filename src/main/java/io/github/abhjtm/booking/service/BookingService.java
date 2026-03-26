package io.github.abhjtm.booking.service;

import io.github.abhjtm.booking.dto.BookingStatus;
import io.github.abhjtm.booking.dto.request.CreateBookingRequest;
import io.github.abhjtm.booking.dto.request.UpdateBookingAttendeesRequest;
import io.github.abhjtm.booking.dto.response.Booking;
import io.github.abhjtm.booking.dto.response.Facility;
import io.github.abhjtm.booking.dto.response.User;
import io.github.abhjtm.booking.exception.InvalidBookingException;
import io.github.abhjtm.booking.repository.BookingRepository;
import io.github.abhjtm.booking.repository.FacilityRepository;
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
    private final UserService userService;
    private final FacilityRepository facilityRepository;

    public BookingService(BookingRepository bookingRepository, UserService userService, FacilityRepository facilityRepository) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.facilityRepository = facilityRepository;
    }

    public Booking createBooking(CreateBookingRequest request) {
        validateTimeSlot(request);
        checkAvailability(request);
        
        // Validate facility exists
        Facility facility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new InvalidBookingException(
                        "FACILITY_NOT_FOUND",
                        "Facility with id " + request.facilityId() + " does not exist"
                ));
        
        // Validate bookedBy user exists and get user ID
        User bookedByUser = userService.getUserByEmail(request.bookedBy())
                .orElseThrow(() -> new InvalidBookingException(
                        "USER_NOT_FOUND",
                        "User with email '" + request.bookedBy() + "' does not exist"
                ));
        
        // Validate booked_by user and facility belong to same apartment
        if (bookedByUser.apartmentId() != facility.apartmentId()) {
            throw new InvalidBookingException(
                    "APARTMENT_MISMATCH",
                    "User and facility must belong to the same apartment"
            );
        }
        
        // Convert attendee emails to user IDs and validate
        List<Integer> requestedAttendeeIds = new ArrayList<>();
        if (request.requestedAttendees() != null && !request.requestedAttendees().isEmpty()) {
            for (String attendeeEmail : request.requestedAttendees()) {
                User attendee = userService.getUserByEmail(attendeeEmail)
                        .orElseThrow(() -> new InvalidBookingException(
                                "ATTENDEE_NOT_FOUND",
                                "Attendee with email '" + attendeeEmail + "' does not exist"
                        ));
                
                if (attendee.apartmentId() != facility.apartmentId()) {
                    throw new InvalidBookingException(
                            "ATTENDEE_APARTMENT_MISMATCH",
                            "Attendee '" + attendeeEmail + "' does not belong to the same apartment as the facility"
                    );
                }
                
                requestedAttendeeIds.add(attendee.id());
            }
        }
        
        // Booked by user is confirmed attendee
        List<Integer> confirmedAttendeeIds = new ArrayList<>();
        confirmedAttendeeIds.add(bookedByUser.id());

        Booking booking = new Booking(
            UUID.randomUUID(),
            request.facilityId(),
            request.description(),
            bookedByUser.id(),
            request.bookedBy(),
            requestedAttendeeIds,
            request.requestedAttendees(),
            confirmedAttendeeIds,
            List.of(request.bookedBy()),
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

    public List<Booking> getBookingsByFacility(int facilityId) {
        return bookingRepository.findByFacilityId(facilityId);
    }

    public List<Booking> getBookingsByUser(String userEmail) {
        // Validate user exists
        userService.validateUserExists(userEmail);
        
        return bookingRepository.findByBookedBy(userEmail);
    }

    public Booking cancelBooking(UUID bookingId, String bookedBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingException("BOOKING_NOT_FOUND", "Booking not found"));

        if (bookedBy == null || bookedBy.isBlank()) {
            throw new InvalidBookingException("BOOKED_BY_REQUIRED", "bookedBy is required");
        }

        if (!booking.bookedByEmail().equals(bookedBy)) {
            throw new InvalidBookingException("BOOKING_FORBIDDEN", "Only bookedBy can cancel this booking");
        }

        if (booking.status() == BookingStatus.CANCELLED || booking.status() == BookingStatus.COMPLETED) {
            throw new InvalidBookingException("BOOKING_NOT_ACTIVE", "Only active bookings can be cancelled");
        }

        Booking cancelled = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedByUserId(),
                booking.bookedByEmail(),
                booking.requestedAttendeeIds(),
                booking.requestedAttendeeEmails(),
                booking.confirmedAttendeeIds(),
                booking.confirmedAttendeeEmails(),
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

        if (!booking.bookedByEmail().equals(request.bookedBy())) {
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

        // Fetch the facility to get its apartment ID
        Facility facility = facilityRepository.findById(booking.facilityId())
                .orElseThrow(() -> new InvalidBookingException(
                        "FACILITY_NOT_FOUND",
                        "Facility with id " + booking.facilityId() + " does not exist"
                ));

        // Convert attendee emails to IDs and validate
        List<Integer> attendeeIdsToAdd = new ArrayList<>();
        List<String> attendeeEmailsToAdd = new ArrayList<>();
        for (String attendeeEmail : attendeesToAdd) {
            User attendee = userService.getUserByEmail(attendeeEmail)
                    .orElseThrow(() -> new InvalidBookingException(
                            "ATTENDEE_NOT_FOUND",
                            "Attendee with email '" + attendeeEmail + "' does not exist"
                    ));

            if (attendee.apartmentId() != facility.apartmentId()) {
                throw new InvalidBookingException(
                        "ATTENDEE_APARTMENT_MISMATCH",
                        "Attendee '" + attendeeEmail + "' does not belong to the same apartment as the facility"
                );
            }
            attendeeIdsToAdd.add(attendee.id());
            attendeeEmailsToAdd.add(attendeeEmail);
        }

        // Convert remove emails to IDs
        List<Integer> attendeeIdsToRemove = new ArrayList<>();
        for (String attendeeEmail : attendeesToRemove) {
            userService.getUserByEmail(attendeeEmail)
                    .ifPresent(user -> attendeeIdsToRemove.add(user.id()));
        }

        // Update attendee lists
        List<Integer> requestedAttendeeIds = new ArrayList<>(booking.requestedAttendeeIds());
        List<String> requestedAttendeeEmails = new ArrayList<>(booking.requestedAttendeeEmails());
        
        attendeeIdsToAdd.forEach(id -> {
            if (!requestedAttendeeIds.contains(id)) {
                requestedAttendeeIds.add(id);
            }
        });
        attendeeEmailsToAdd.forEach(email -> {
            if (!requestedAttendeeEmails.contains(email)) {
                requestedAttendeeEmails.add(email);
            }
        });
        
        requestedAttendeeIds.removeAll(attendeeIdsToRemove);
        requestedAttendeeEmails.removeAll(attendeesToRemove);

        List<Integer> confirmedAttendeeIds = new ArrayList<>(booking.confirmedAttendeeIds());
        List<String> confirmedAttendeeEmails = new ArrayList<>(booking.confirmedAttendeeEmails());
        confirmedAttendeeIds.removeAll(attendeeIdsToRemove);
        confirmedAttendeeEmails.removeAll(attendeesToRemove);

        Booking updated = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedByUserId(),
                booking.bookedByEmail(),
                requestedAttendeeIds,
                requestedAttendeeEmails,
                confirmedAttendeeIds,
                confirmedAttendeeEmails,
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

        if (!booking.requestedAttendeeEmails().contains(attendeeEmail)) {
            throw new InvalidBookingException(
                    "ATTENDEE_NOT_REQUESTED",
                    "Only requested attendees can confirm attendance"
            );
        }

        // Get attendee user ID
        User attendee = userService.getUserByEmail(attendeeEmail)
                .orElseThrow(() -> new InvalidBookingException(
                        "ATTENDEE_NOT_FOUND",
                        "Attendee with email '" + attendeeEmail + "' does not exist"
                ));

        List<Integer> confirmedAttendeeIds = new ArrayList<>(booking.confirmedAttendeeIds());
        List<String> confirmedAttendeeEmails = new ArrayList<>(booking.confirmedAttendeeEmails());
        
        if (!confirmedAttendeeIds.contains(attendee.id())) {
            confirmedAttendeeIds.add(attendee.id());
            confirmedAttendeeEmails.add(attendeeEmail);
        }

        Booking updated = new Booking(
                booking.id(),
                booking.facilityId(),
                booking.description(),
                booking.bookedByUserId(),
                booking.bookedByEmail(),
                booking.requestedAttendeeIds(),
                booking.requestedAttendeeEmails(),
                confirmedAttendeeIds,
                confirmedAttendeeEmails,
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
