package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.User;
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
public class MySqlUserRepository implements UserRepository {

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                flat_number VARCHAR(50) NOT NULL,
                apartment_id INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_users_email (email),
                INDEX idx_users_apartment_id (apartment_id),
                CONSTRAINT fk_users_apartment_id FOREIGN KEY (apartment_id) REFERENCES apartments(id)
            )
            """;

    private static final String INSERT_USER = """
            INSERT INTO users (name, email, flat_number, apartment_id)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, email, flat_number, apartment_id
            FROM users
            WHERE id = ?
            """;

    private static final String SELECT_BY_EMAIL = """
            SELECT id, name, email, flat_number, apartment_id
            FROM users
            WHERE email = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, name, email, flat_number, apartment_id
            FROM users
            """;

    private static final String SELECT_BY_APARTMENT_ID = """
            SELECT id, name, email, flat_number, apartment_id
            FROM users
            WHERE apartment_id = ?
            """;

    private static final String DELETE_BY_ID = "DELETE FROM users WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = this::mapRow;

    public MySqlUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_USERS_TABLE);
    }

    @Override
    public User save(User user) {
        jdbcTemplate.update(
                INSERT_USER,
                user.name(),
                user.email(),
                user.flatNumber(),
                user.apartmentId()
        );
        return user;
    }

    @Override
    public Optional<User> findById(int id) {
        return jdbcTemplate.query(SELECT_BY_ID, userRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(SELECT_BY_EMAIL, userRowMapper, email)
                .stream()
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return jdbcTemplate.query(SELECT_ALL, userRowMapper);
    }

    @Override
    public List<User> findByApartmentId(int apartmentId) {
        return jdbcTemplate.query(SELECT_BY_APARTMENT_ID, userRowMapper, apartmentId);
    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private User mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("flat_number"),
                resultSet.getInt("apartment_id")
        );
    }
}
