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
                booked_by VARCHAR(255) NOT NULL,
                requested_attendees JSON NOT NULL,
                confirmed_attendees JSON NOT NULL,
                start_time VARCHAR(40) NOT NULL,
                end_time VARCHAR(40) NOT NULL,
                status VARCHAR(32) NOT NULL,
                created_at VARCHAR(40) NOT NULL,
                updated_at VARCHAR(40) NOT NULL,
                INDEX idx_bookings_facility_id_start_end (facility_id, start_time, end_time)
            )
            """;

    private static final String UPSERT_BOOKING = """
            INSERT INTO bookings (
                id,
                facility_id,
                description,
                booked_by,
                requested_attendees,
                confirmed_attendees,
                start_time,
                end_time,
                status,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                facility_id = VALUES(facility_id),
                description = VALUES(description),
                booked_by = VALUES(booked_by),
                requested_attendees = VALUES(requested_attendees),
                confirmed_attendees = VALUES(confirmed_attendees),
                start_time = VALUES(start_time),
                end_time = VALUES(end_time),
                status = VALUES(status),
                updated_at = VALUES(updated_at)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, facility_id, description, booked_by, requested_attendees, confirmed_attendees,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, facility_id, description, booked_by, requested_attendees, confirmed_attendees,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            """;

    private static final String SELECT_BY_FACILITY_ID = """
            SELECT id, facility_id, description, booked_by, requested_attendees, confirmed_attendees,
                   start_time, end_time, status, created_at, updated_at
            FROM bookings
            WHERE facility_id = ?
            """;

    private static final String DELETE_BY_ID = "DELETE FROM bookings WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<Booking> bookingRowMapper = this::mapRow;

    public MySqlBookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_BOOKINGS_TABLE);
    }

    @Override
    public Booking save(Booking booking) {
        jdbcTemplate.update(
                UPSERT_BOOKING,
                booking.id().toString(),
                booking.facilityId(),
                booking.description(),
                booking.bookedBy(),
                toJson(booking.requestedAttendees()),
                toJson(booking.confirmedAttendees()),
                booking.startTime().toString(),
                booking.endTime().toString(),
                booking.status().name(),
                booking.createdAt().toString(),
                booking.updatedAt().toString()
        );

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
    public void deleteById(UUID id) {
        jdbcTemplate.update(DELETE_BY_ID, id.toString());
    }

    private Booking mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Booking(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getInt("facility_id"),
                resultSet.getString("description"),
                resultSet.getString("booked_by"),
                fromJson(resultSet.getString("requested_attendees")),
                fromJson(resultSet.getString("confirmed_attendees")),
                OffsetDateTime.parse(resultSet.getString("start_time")),
                OffsetDateTime.parse(resultSet.getString("end_time")),
                BookingStatus.valueOf(resultSet.getString("status")),
                OffsetDateTime.parse(resultSet.getString("created_at")),
                OffsetDateTime.parse(resultSet.getString("updated_at"))
        );
    }

    private String toJson(List<String> attendees) {
        try {
            return objectMapper.writeValueAsString(attendees == null ? List.of() : attendees);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize attendees", e);
        }
    }

    private List<String> fromJson(String attendeesJson) {
        try {
            return objectMapper.readValue(attendeesJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize attendees", e);
        }
    }
}

