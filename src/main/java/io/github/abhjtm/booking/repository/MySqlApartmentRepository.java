package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Apartment;
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
public class MySqlApartmentRepository implements ApartmentRepository {

    private static final String CREATE_APARTMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS apartments (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                pincode VARCHAR(10) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String INSERT_APARTMENT = """
            INSERT INTO apartments (name, pincode)
            VALUES (?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, pincode
            FROM apartments
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, name, pincode
            FROM apartments
            """;

    private static final String DELETE_BY_ID = "DELETE FROM apartments WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Apartment> apartmentRowMapper = this::mapRow;

    public MySqlApartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_APARTMENTS_TABLE);
    }

    @Override
    public Apartment save(Apartment apartment) {
        jdbcTemplate.update(
                INSERT_APARTMENT,
                apartment.name(),
                apartment.pincode()
        );
        return apartment;
    }

    @Override
    public Optional<Apartment> findById(int id) {
        return jdbcTemplate.query(SELECT_BY_ID, apartmentRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Apartment> findAll() {
        return jdbcTemplate.query(SELECT_ALL, apartmentRowMapper);
    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private Apartment mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Apartment(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("pincode")
        );
    }
}
