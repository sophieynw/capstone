package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CreatePropertyRequest;
import ca.sheridancollege.restfulhousekeeping.models.PropertyResponse;
import ca.sheridancollege.restfulhousekeeping.models.UpdatePropertyRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.services.CleaningPhotoService;
import ca.sheridancollege.restfulhousekeeping.services.ImageKitService;
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
    private final ImageKitService imageKitService;
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

        return propertyRepository.findByIdAndManager_Id(id, manager.getId())
                .map(property -> {
                    try {
                        String previousFileId = property.getPropertyPictureFileId();
                        ImageKitService.UploadedImage uploaded = imageKitService.upload(
                                file,
                                "/restful-housekeeping/property-pictures"
                        );

                        property.setPropertyPicturePath(uploaded.url());
                        property.setPropertyPictureFileId(uploaded.fileId());
                        propertyRepository.save(property);
                        imageKitService.delete(previousFileId);

                        PropertyResponse response = propertyResponseService
                                .getPropertyById(id, manager.getId())
                                .orElseThrow();
                        return ResponseEntity.ok(response);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
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
                    propertyRepository.delete(property);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
