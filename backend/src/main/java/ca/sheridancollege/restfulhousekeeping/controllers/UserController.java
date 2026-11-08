package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.AvailabilitySlotResponse;
import ca.sheridancollege.restfulhousekeeping.models.UpdateUserRequest;
import ca.sheridancollege.restfulhousekeeping.models.UserResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.SlotRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import ca.sheridancollege.restfulhousekeeping.services.ImageKitService;
import ca.sheridancollege.restfulhousekeeping.services.UserResponseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cleaners")
@AllArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final UserResponseService userResponseService;
    private final ImageKitService imageKitService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{organizationId}/cleaners")
    public ResponseEntity<List<UserResponse>> getCleaners(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal User requester) {
        List<UserResponse> cleaners = userRepository
                .findAllByOrganization_IdAndRole(organizationId, Role.CLEANER)
                .stream().map(UserResponse::fromUser).toList();

        if (!cleaners.isEmpty() && requester != null && requester.getRole() == Role.MANAGER
                && requester.getOrganization() != null
                && organizationId.equals(requester.getOrganization().getId())) {
            var slotsByCleaner = slotRepository
                    .findByCleanerIdIn(cleaners.stream().map(UserResponse::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(slot -> slot.getCleaner().getId(),
                            Collectors.mapping(AvailabilitySlotResponse::fromSlot, Collectors.toList())));
            cleaners.forEach(cleaner -> cleaner.setAvailabilitySlots(
                    slotsByCleaner.getOrDefault(cleaner.getId(), List.of())));
        }

        return ResponseEntity.ok(cleaners);
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody UpdateUserRequest updated) {
        return userResponseService.updateUser(id, updated)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}

    // upload a profile picture for a user
    @PostMapping(value = "/{id}/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String previousFileId = user.getProfilePictureFileId();
        ImageKitService.UploadedImage uploaded = imageKitService.upload(
                file,
                "/restful-housekeeping/profile-pictures"
        );

        user.setProfilePicturePath(uploaded.url());
        user.setProfilePictureFileId(uploaded.fileId());
        userRepository.save(user);
        imageKitService.delete(previousFileId);

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    // get profile picture path for a user
    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<String> getProfilePicture(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user.getProfilePicturePath());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User manager) {
        userResponseService.removeCleanerFromTeam(id, manager);
        return ResponseEntity.noContent().build();
    }
}
