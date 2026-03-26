package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Facility;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "mysql", matchIfMissing = true)
public class MySqlFacilityRepository implements FacilityRepository {

    private static final String CREATE_FACILITIES_TABLE = """
            CREATE TABLE IF NOT EXISTS facilities (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                apartment_id INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_facilities_apartment_id (apartment_id),
                CONSTRAINT fk_facilities_apartment_id FOREIGN KEY (apartment_id) REFERENCES apartments(id)
            )
            """;

    private static final String INSERT_FACILITY = """
            INSERT INTO facilities (name, description, apartment_id)
            VALUES (?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, description, apartment_id
            FROM facilities
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, name, description, apartment_id
            FROM facilities
            """;

    private static final String SELECT_BY_APARTMENT_ID = """
            SELECT id, name, description, apartment_id
            FROM facilities
            WHERE apartment_id = ?
            """;

    private static final String DELETE_BY_ID = "DELETE FROM facilities WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Facility> facilityRowMapper = this::mapRow;

    public MySqlFacilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_FACILITIES_TABLE);
    }

    @Override
    public Facility save(Facility facility) {
        jdbcTemplate.update(
                INSERT_FACILITY,
                facility.name(),
                facility.description(),
                facility.apartmentId()
        );
        return facility;
    }

    @Override
    public Optional<Facility> findById(int id) {
        return jdbcTemplate.query(SELECT_BY_ID, facilityRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Facility> findAll() {
        return jdbcTemplate.query(SELECT_ALL, facilityRowMapper);
    }

    @Override
    public List<Facility> findByApartmentId(int apartmentId) {
        return jdbcTemplate.query(SELECT_BY_APARTMENT_ID, facilityRowMapper, apartmentId);
    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private Facility mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Facility(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("apartment_id")
        );
    }
}
