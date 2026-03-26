package io.github.abhjtm.booking.service;

import io.github.abhjtm.booking.dto.request.CreateApartmentRequest;
import io.github.abhjtm.booking.dto.response.Apartment;
import io.github.abhjtm.booking.exception.InvalidBookingException;
import io.github.abhjtm.booking.repository.ApartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    public Apartment createApartment(CreateApartmentRequest request) {
        validateApartmentRequest(request);

        Apartment apartment = new Apartment(
            0,
            request.name(),
            request.pincode()
        );

        return apartmentRepository.save(apartment);
    }

    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }

    public Optional<Apartment> getApartmentById(int id) {
        return apartmentRepository.findById(id);
    }

    public void deleteApartment(int id) {
        if (apartmentRepository.findById(id).isEmpty()) {
            throw new InvalidBookingException("APARTMENT_NOT_FOUND", "Apartment with id " + id + " does not exist");
        }
        apartmentRepository.deleteById(id);
    }

    private void validateApartmentRequest(CreateApartmentRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidBookingException("APARTMENT_NAME_REQUIRED", "Apartment name is required");
        }
        if (request.pincode() == null || request.pincode().isBlank()) {
            throw new InvalidBookingException("APARTMENT_PINCODE_REQUIRED", "Pincode is required");
        }
    }
}

