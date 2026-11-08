package ca.sheridancollege.restfulhousekeeping.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ca.sheridancollege.restfulhousekeeping.beans.Availability;
import ca.sheridancollege.restfulhousekeeping.repositories.AvailabilityRepository;

@RestController
@RequestMapping("/availabilities")
public class AvailabilityController {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityController(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @GetMapping
    public List<Availability> getAll() {
        return availabilityRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Availability> getById(@PathVariable Long id) {
        return availabilityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Availability create(@RequestBody Availability availability) {
        return availabilityRepository.save(availability);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!availabilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        availabilityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}