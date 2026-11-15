package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CreatePropertyRequest;
import ca.sheridancollege.restfulhousekeeping.models.PropertyResponse;
import ca.sheridancollege.restfulhousekeeping.models.UpdatePropertyRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.services.CleaningPhotoService;
import ca.sheridancollege.restfulhousekeeping.services.PhotoService;
import ca.sheridancollege.restfulhousekeeping.services.PropertyResponseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/properties")
@SecurityRequirement(name = "Bearer Authentication")
public class PropertyController {

    private final PropertyRepository propertyRepository;
    private final PropertyResponseService propertyResponseService;
    private final PhotoService photoService;
    private final CleaningRepository cleaningRepository;
    private final CleaningPhotoService cleaningPhotoService;

    // GET ALL properties for authenticated manager
    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getAll(@AuthenticationPrincipal User manager) {
        List<PropertyResponse> properties = propertyResponseService.getPropertiesForManager(manager.getId());
        return ResponseEntity.ok(properties);
    }

    // GET property by propertyId, scoped to manager's own properties
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getById(@PathVariable Long id, @AuthenticationPrincipal User manager) {
        return propertyResponseService.getPropertyById(id, manager.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE new property record
    @PostMapping
    public ResponseEntity<PropertyResponse> create(
            @RequestBody CreatePropertyRequest request) {

        PropertyResponse response =
                propertyResponseService.createProperty(
                        request.getProperty(),
                        request.getChecklistItems()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PropertyResponse> update(@PathVariable Long id, @RequestBody UpdatePropertyRequest updated) {
        return propertyResponseService.updateProperty(id, updated)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/picture", consumes = "multipart/form-data")
    public ResponseEntity<PropertyResponse> uploadPicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User manager) throws IOException {

        return ResponseEntity.ok(photoService.replacePropertyPhoto(id, file, manager));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User manager) {
        return propertyRepository.findByIdForUpdate(id)
                .filter(property -> property.getManager().getId().equals(manager.getId()))
                .map(property -> {
                    List<Long> cleaningIds = property.getCleanings().stream().map(Cleaning::getId).sorted().toList();
                    cleaningIds.forEach(cleaningRepository::findByIdForUpdate);
                    cleaningPhotoService.deleteCloudPhotosAfterCleaningRemoval(cleaningIds);
                    if (property.getPropertyPhoto() != null) {
                        photoService.deleteAfterCommit(property.getPropertyPhoto().getImageKitFileId());
                    }
                    propertyRepository.delete(property);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
