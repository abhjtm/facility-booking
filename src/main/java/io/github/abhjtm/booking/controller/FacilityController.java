package io.github.abhjtm.booking.controller;

import io.github.abhjtm.booking.dto.request.CreateFacilityRequest;
import io.github.abhjtm.booking.dto.response.Facility;
import io.github.abhjtm.booking.service.FacilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    /**
     * Create a new facility
     * @param request CreateFacilityRequest with name, description, and apartmentId (must exist in apartments table)
     * @return Created facility with assigned ID
     */
    @PostMapping
    public ResponseEntity<Facility> createFacility(@RequestBody CreateFacilityRequest request) {
        Facility facility = facilityService.createFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(facility);
    }


    /**
     * Get facilities by apartment ID
     * @param apartmentId The apartment ID to filter facilities
     * @return List of facilities in the specified apartment
     */
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<Facility>> getFacilitiesByApartment(@PathVariable int apartmentId) {
        List<Facility> facilities = facilityService.getFacilitiesByApartment(apartmentId);
        return ResponseEntity.ok(facilities);
    }

    /**
     * Delete a facility
     * @param id The facility ID to delete
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacility(@PathVariable int id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.noContent().build();
    }
}

