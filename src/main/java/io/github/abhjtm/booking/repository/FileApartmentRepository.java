package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Apartment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "file")
public class FileApartmentRepository implements ApartmentRepository {

    private final ConcurrentHashMap<Integer, Apartment> apartments = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    @Override
    public Apartment save(Apartment apartment) {
        int id = apartment.id() > 0 ? apartment.id() : idSequence.getAndIncrement();
        Apartment persisted = new Apartment(id, apartment.name(), apartment.pincode());
        apartments.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<Apartment> findById(int id) {
        return Optional.ofNullable(apartments.get(id));
    }

    @Override
    public List<Apartment> findAll() {
        return apartments.values().stream().toList();
    }

    @Override
    public void deleteById(int id) {
        apartments.remove(id);
    }
}
