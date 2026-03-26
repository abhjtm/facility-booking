package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "file")
public class FileUserRepository implements UserRepository {

    private final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    @Override
    public User save(User user) {
        int id = user.id() > 0 ? user.id() : idSequence.getAndIncrement();
        User persisted = new User(id, user.name(), user.email(), user.flatNumber(), user.apartmentId());
        users.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<User> findById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return users.values().stream().toList();
    }

    @Override
    public List<User> findByApartmentId(int apartmentId) {
        return users.values().stream()
                .filter(u -> u.apartmentId() == apartmentId)
                .toList();
    }

    @Override
    public void deleteById(int id) {
        users.remove(id);
    }
}

