package io.github.abhjtm.booking.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.abhjtm.booking.dto.BookingStatus;
import io.github.abhjtm.booking.dto.response.Booking;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "mysql", matchIfMissing = true)
public class MySqlBookingRepository implements BookingRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private static final String CREATE_BOOKINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS bookings (
                id CHAR(36) PRIMARY KEY,
                facility_id INT NOT NULL,
                description TEXT NULL,
                booked_by_user_id INT NOT NULL,
                start_time VARCHAR(40) NOT NULL,
                end_time VARCHAR(40) NOT NULL,
                status VARCHAR(32) NOT NULL,
                created_at VARCHAR(40) NOT NULL,
                updated_at VARCHAR(40) NOT NULL,
                INDEX idx_bookings_facility_id_start_end (facility_id, start_time, end_time),
                INDEX idx_bookings_booked_by_user_id (booked_by_user_id),
                CONSTRAINT fk_bookings_booked_by_user_id FOREIGN KEY (booked_by_user_id) REFERENCES users(id)
            )
            """;

    private static final String CREATE_BOOKING_ATTENDEES_TABLE = """
            CREATE TABLE IF NOT EXISTS booking_attendees (
                booking_id CHAR(36) NOT NULL,
                user_id INT NOT NULL,
                is_confirmed BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (booking_id, user_id),
                CONSTRAINT fk_booking_attendees_booking_id FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
                CONSTRAINT fk_booking_attendees_user_id FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """;

    private static final String UPSERT_BOOKING = """
            INSERT INTO bookings (
                id,
                facility_id,
                description,
                booked_by_user_id,
                start_time,
                end_time,
                status,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                facility_id = VALUES(facility_id),
                description = VALUES(description),
                booked_by_user_id = VALUES(booked_by_user_id),
                start_time = VALUES(start_time),
                end_time = VALUES(end_time),
                status = VALUES(status),
                updated_at = VALUES(updated_at)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, facility_id, description, booked_by_user_id,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, facility_id, description, booked_by_user_id,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            """;

    private static final String SELECT_BY_FACILITY_ID = """
            SELECT id, facility_id, description, booked_by_user_id,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            WHERE facility_id = ?
            """;

    private static final String SELECT_BY_BOOKED_BY = """
            SELECT id, facility_id, description, booked_by_user_id,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            WHERE booked_by_user_id = (SELECT id FROM users WHERE email = ?)
            ORDER BY start_time DESC
            """;

    private static final String DELETE_BY_ID = "DELETE FROM bookings WHERE id = ?";

    private static final String GET_BOOKING_ATTENDEES = """
            SELECT user_id, is_confirmed FROM booking_attendees WHERE booking_id = ?
            """;

    private static final String INSERT_BOOKING_ATTENDEE = """
            INSERT INTO booking_attendees (booking_id, user_id, is_confirmed) 
            VALUES (?, ?, ?)
            """;

    private static final String DELETE_BOOKING_ATTENDEES = """
            DELETE FROM booking_attendees WHERE booking_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RowMapper<Booking> bookingRowMapper = this::mapRow;

    public MySqlBookingRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_BOOKINGS_TABLE);
        jdbcTemplate.execute(CREATE_BOOKING_ATTENDEES_TABLE);
    }

    @Override
    public Booking save(Booking booking) {
        // Delete existing attendees for this booking
        jdbcTemplate.update(DELETE_BOOKING_ATTENDEES, booking.id().toString());
        
        // Insert/update booking
        jdbcTemplate.update(
                UPSERT_BOOKING,
                booking.id().toString(),
                booking.facilityId(),
                booking.description(),
                booking.bookedByUserId(),
                booking.startTime().toString(),
                booking.endTime().toString(),
                booking.status().name(),
                booking.createdAt().toString(),
                booking.updatedAt().toString()
        );

        // Insert requested attendees
        if (booking.requestedAttendeeIds() != null && !booking.requestedAttendeeIds().isEmpty()) {
            for (int userId : booking.requestedAttendeeIds()) {
                boolean isConfirmed = booking.confirmedAttendeeIds() != null && 
                                    booking.confirmedAttendeeIds().contains(userId);
                jdbcTemplate.update(INSERT_BOOKING_ATTENDEE, booking.id().toString(), userId, isConfirmed);
            }
        }

        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jdbcTemplate.query(SELECT_BY_ID, bookingRowMapper, id.toString())
                .stream()
                .findFirst();
    }

    @Override
    public List<Booking> findAll() {
        return jdbcTemplate.query(SELECT_ALL, bookingRowMapper)
                .stream()
                .sorted(Comparator.comparing(Booking::startTime).thenComparing(Booking::createdAt))
                .toList();
    }

    @Override
    public List<Booking> findByFacilityId(int facilityId) {
        return jdbcTemplate.query(SELECT_BY_FACILITY_ID, bookingRowMapper, facilityId)
                .stream()
                .sorted(Comparator.comparing(Booking::startTime).thenComparing(Booking::createdAt))
                .toList();
    }

    @Override
    public List<Booking> findByBookedBy(String bookedByEmail) {
        return jdbcTemplate.query(SELECT_BY_BOOKED_BY, bookingRowMapper, bookedByEmail)
                .stream()
                .sorted(Comparator.comparing(Booking::startTime).thenComparing(Booking::createdAt))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update(DELETE_BY_ID, id.toString());
    }

    private Booking mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        String bookingId = resultSet.getString("id");
        int bookedByUserId = resultSet.getInt("booked_by_user_id");
        
        // Get booked by user email
        String bookedByEmail = userRepository.findById(bookedByUserId)
                .map(user -> user.email())
                .orElse("");
        
        // Get attendees from junction table
        List<Integer> requestedAttendeeIds = new java.util.ArrayList<>();
        List<Integer> confirmedAttendeeIds = new java.util.ArrayList<>();
        List<String> requestedAttendeeEmails = new java.util.ArrayList<>();
        List<String> confirmedAttendeeEmails = new java.util.ArrayList<>();
        
        List<java.util.Map<String, Object>> attendeeRows = jdbcTemplate.queryForList(
                GET_BOOKING_ATTENDEES, bookingId
        );
        
        for (java.util.Map<String, Object> row : attendeeRows) {
            int userId = ((Number) row.get("user_id")).intValue();
            boolean isConfirmed = ((Boolean) row.get("is_confirmed"));
            
            requestedAttendeeIds.add(userId);
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                String email = userOpt.get().email();
                requestedAttendeeEmails.add(email);
                if (isConfirmed) {
                    confirmedAttendeeIds.add(userId);
                    confirmedAttendeeEmails.add(email);
                }
            }
        }
        
        return new Booking(
                UUID.fromString(bookingId),
                resultSet.getInt("facility_id"),
                resultSet.getString("description"),
                bookedByUserId,
                bookedByEmail,
                requestedAttendeeIds,
                requestedAttendeeEmails,
                confirmedAttendeeIds,
                confirmedAttendeeEmails,
                OffsetDateTime.parse(resultSet.getString("start_time")),
                OffsetDateTime.parse(resultSet.getString("end_time")),
                BookingStatus.valueOf(resultSet.getString("status")),
                OffsetDateTime.parse(resultSet.getString("created_at")),
                OffsetDateTime.parse(resultSet.getString("updated_at"))
        );
    }
}

