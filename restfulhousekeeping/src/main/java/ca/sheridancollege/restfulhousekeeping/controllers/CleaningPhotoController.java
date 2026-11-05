package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse;
import ca.sheridancollege.restfulhousekeeping.services.CleaningPhotoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/photos")
@SecurityRequirement(name = "Bearer Authentication")
public class CleaningPhotoController {

    private final CleaningPhotoService photoService;

    @GetMapping("/cleaning/{cleaningId}")
    public List<CleaningPhotoResponse> list(@PathVariable Long cleaningId, @AuthenticationPrincipal User user) {
        return photoService.list(cleaningId, user);
    }

    @PostMapping(value = "/cleaning/{cleaningId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CleaningPhotoResponse upload(@PathVariable Long cleaningId, @RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal User user) throws IOException {
        return photoService.upload(cleaningId, file, user);
    }

    @GetMapping("/cleaning/{cleaningId}/{photoId}")
    public ResponseEntity<Resource> read(@PathVariable Long cleaningId, @PathVariable Long photoId,
                                         @AuthenticationPrincipal User user) throws IOException {
        return imageResponse(photoService.read(cleaningId, photoId, user));
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> readByFileName(@PathVariable String fileName,
                                                   @AuthenticationPrincipal User user) throws IOException {
        return imageResponse(photoService.readByFileName(fileName, user));
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long photoId, @AuthenticationPrincipal User user) {
        photoService.delete(photoId, user);
    }

    private static ResponseEntity<Resource> imageResponse(CleaningPhotoService.ImageContent image) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(image.mediaType())
                .body(image.resource());
    }
}
