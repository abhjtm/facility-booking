package io.github.abhjtm.booking.controller;

import io.github.abhjtm.booking.dto.request.CreateApartmentRequest;
import io.github.abhjtm.booking.dto.response.Apartment;
import io.github.abhjtm.booking.service.ApartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @PostMapping
    public ResponseEntity<Apartment> createApartment(@RequestBody CreateApartmentRequest request) {
        Apartment apartment = apartmentService.createApartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(apartment);
    }

    @GetMapping
    public ResponseEntity<List<Apartment>> getAllApartments() {
        List<Apartment> apartments = apartmentService.getAllApartments();
        return ResponseEntity.ok(apartments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartment(@PathVariable int id) {
        apartmentService.deleteApartment(id);
        return ResponseEntity.noContent().build();
    }
}

