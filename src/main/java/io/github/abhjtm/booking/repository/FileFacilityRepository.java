package io.github.abhjtm.booking.repository;

import io.github.abhjtm.booking.dto.response.Facility;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@ConditionalOnProperty(name = "booking.storage.type", havingValue = "file")
public class FileFacilityRepository implements FacilityRepository {

    private final ConcurrentHashMap<Integer, Facility> facilities = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    @Override
    public Facility save(Facility facility) {
        int id = facility.id() > 0 ? facility.id() : idSequence.getAndIncrement();
        Facility persisted = new Facility(id, facility.name(), facility.description(), facility.apartmentId());
        facilities.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<Facility> findById(int id) {
        return Optional.ofNullable(facilities.get(id));
    }

    @Override
    public List<Facility> findAll() {
        return facilities.values().stream().toList();
    }

    @Override
    public List<Facility> findByApartmentId(int apartmentId) {
        return facilities.values().stream()
                .filter(f -> f.apartmentId() == apartmentId)
                .toList();
    }

    @Override
    public void deleteById(int id) {
        facilities.remove(id);
    }
}

