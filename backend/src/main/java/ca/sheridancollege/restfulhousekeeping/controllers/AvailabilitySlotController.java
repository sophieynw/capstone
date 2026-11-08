package ca.sheridancollege.restfulhousekeeping.controllers;

import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.beans.AvailabilitySlot;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.AvailabilitySlotRequest;
import ca.sheridancollege.restfulhousekeeping.models.AvailabilitySlotUpdateRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.SlotRepository;

@RestController
@RequestMapping("/availability-slots")
public class AvailabilitySlotController {

    private final SlotRepository slotRepository;

    public AvailabilitySlotController(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @GetMapping
    public List<AvailabilitySlot> getAll(@AuthenticationPrincipal User cleaner) {
        return slotRepository.findByCleanerId(cleaner.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvailabilitySlot> getById(
            @PathVariable Long id, @AuthenticationPrincipal User cleaner) {
        return slotRepository.findByIdAndCleanerId(id, cleaner.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cleaner/{cleanerId}")
    public List<AvailabilitySlot> getByCleanerId(
            @PathVariable Long cleanerId, @AuthenticationPrincipal User cleaner) {
        if (!cleaner.getId().equals(cleanerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return slotRepository.findByCleanerId(cleaner.getId());
    }

    @PostMapping
    public ResponseEntity<AvailabilitySlot> create(
            @AuthenticationPrincipal User cleaner,
            @RequestBody AvailabilitySlotRequest request) {
        if (request.dayOfWeek() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Day of week is required.");
        }
        validateTimes(request.startTime(), request.endTime());

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setCleaner(cleaner);
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        return ResponseEntity.status(HttpStatus.CREATED).body(slotRepository.save(slot));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AvailabilitySlot> update(
            @PathVariable Long id, @AuthenticationPrincipal User cleaner,
            @RequestBody AvailabilitySlotUpdateRequest request) {
        validateTimes(request.startTime(), request.endTime());
        return slotRepository.findByIdAndCleanerId(id, cleaner.getId()).map(existing -> {
            existing.setStartTime(request.startTime());
            existing.setEndTime(request.endTime());
            return ResponseEntity.ok(slotRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User cleaner) {
        return slotRepository.findByIdAndCleanerId(id, cleaner.getId()).map(existing -> {
            slotRepository.delete(existing);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private static void validateTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start and end times are required, and end time must be later than start time.");
        }
    }
}
