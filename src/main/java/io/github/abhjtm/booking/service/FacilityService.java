package io.github.abhjtm.booking.service;

import io.github.abhjtm.booking.dto.request.CreateFacilityRequest;
import io.github.abhjtm.booking.dto.response.Facility;
import io.github.abhjtm.booking.exception.InvalidBookingException;
import io.github.abhjtm.booking.repository.ApartmentRepository;
import io.github.abhjtm.booking.repository.FacilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final ApartmentRepository apartmentRepository;

    public FacilityService(FacilityRepository facilityRepository, ApartmentRepository apartmentRepository) {
        this.facilityRepository = facilityRepository;
        this.apartmentRepository = apartmentRepository;
    }

    public Facility createFacility(CreateFacilityRequest request) {
        validateFacilityRequest(request);

        // Validate apartment exists
        if (apartmentRepository.findById(request.apartmentId()).isEmpty()) {
            throw new InvalidBookingException(
                    "APARTMENT_NOT_FOUND",
                    "Apartment with id " + request.apartmentId() + " does not exist"
            );
        }

        Facility facility = new Facility(
            0,
            request.name(),
            request.description(),
            request.apartmentId()
        );

        return facilityRepository.save(facility);
    }

    public List<Facility> getAllFacilities() {
        return facilityRepository.findAll();
    }

    public Optional<Facility> getFacilityById(int id) {
        return facilityRepository.findById(id);
    }

    public List<Facility> getFacilitiesByApartment(int apartmentId) {
        return facilityRepository.findByApartmentId(apartmentId);
    }

    public void deleteFacility(int id) {
        if (facilityRepository.findById(id).isEmpty()) {
            throw new InvalidBookingException("FACILITY_NOT_FOUND", "Facility with id " + id + " does not exist");
        }
        facilityRepository.deleteById(id);
    }

    private void validateFacilityRequest(CreateFacilityRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidBookingException("FACILITY_NAME_REQUIRED", "Facility name is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new InvalidBookingException("FACILITY_DESCRIPTION_REQUIRED", "Facility description is required");
        }
        if (request.apartmentId() <= 0) {
            throw new InvalidBookingException("FACILITY_APARTMENT_ID_REQUIRED", "Valid apartment ID is required");
        }
    }
}

