package io.github.abhjtm.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.abhjtm.booking.dto.response.Booking;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based implementation of BookingRepository.
 * Stores bookings as JSON Lines (one JSON object per line).
 * Can be replaced with JpaBookingRepository in future.
 */
@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "file")
public class FileBookingRepository implements BookingRepository {

    private final ObjectMapper objectMapper;
    private final Path filePath;

    public FileBookingRepository(
            @Value("${booking.storage.file:bookings.jsonl}") String fileName) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.filePath = Paths.get(fileName);
    }

    @PostConstruct
    public void init() throws IOException {
        if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }

    @Override
    public Booking save(Booking booking) {
        try {
            List<Booking> existing = findAll();
            boolean updated = false;

            for (int i = 0; i < existing.size(); i++) {
                if (existing.get(i).id().equals(booking.id())) {
                    existing.set(i, booking);
                    updated = true;
                    break;
                }
            }

            if (updated) {
                rewriteFile(existing);
            } else {
                String json = objectMapper.writeValueAsString(booking);
                Files.writeString(filePath, json + System.lineSeparator(),
                        StandardOpenOption.APPEND);
            }

            return booking;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save booking", e);
        }
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return findAll().stream()
                .filter(b -> b.id().equals(id))
                .findFirst();
    }

    @Override
    public List<Booking> findAll() {
        try {
            if (!Files.exists(filePath)) {
                return Collections.emptyList();
            }
            return Files.readAllLines(filePath).stream()
                    .filter(line -> !line.isBlank())
                    .map(this::parseBooking)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bookings", e);
        }
    }

    @Override
    public List<Booking> findByFacilityId(int facilityId) {
        return findAll().stream()
                .filter(b -> b.facilityId() == facilityId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        try {
            List<Booking> bookings = findAll().stream()
                    .filter(b -> !b.id().equals(id))
                    .collect(Collectors.toList());
            rewriteFile(bookings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete booking", e);
        }
    }

    private Booking parseBooking(String json) {
        try {
            return objectMapper.readValue(json, Booking.class);
        } catch (IOException e) {
            // Log and skip corrupted lines
            System.err.println("Failed to parse booking: " + e.getMessage());
            return null;
        }
    }

    private void rewriteFile(List<Booking> bookings) throws IOException {
        List<String> lines = bookings.stream()
                .map(b -> {
                    try {
                        return objectMapper.writeValueAsString(b);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
