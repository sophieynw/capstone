package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Photo;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.PropertyResponse;
import ca.sheridancollege.restfulhousekeeping.models.UserResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.PhotoRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {

    private final ImageKitService imageKitService;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyResponseService propertyResponseService;

    @Transactional
    public UserResponse replaceProfilePhoto(Long userId, MultipartFile file, User requester) throws IOException {
        if (requester == null || !userId.equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change your own profile photo");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Photo photo = uploadReplacement(file, "/restful-housekeeping/profile-pictures/" + userId,
                user.getProfilePhoto());
        user.setProfilePhoto(photo);
        return UserResponse.fromUser(user);
    }

    @Transactional
    public PropertyResponse replacePropertyPhoto(Long propertyId, MultipartFile file, User manager) throws IOException {
        Property property = propertyRepository.findByIdForUpdate(propertyId)
                .filter(candidate -> candidate.getManager().getId().equals(manager.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        String folder = "/restful-housekeeping/property-pictures/" + manager.getId() + "/" + propertyId;
        property.setPropertyPhoto(uploadReplacement(file, folder, property.getPropertyPhoto()));
        return propertyResponseService.getPropertyById(propertyId, manager.getId()).orElseThrow();
    }

    private Photo uploadReplacement(MultipartFile file, String folder, Photo previous) throws IOException {
        ImageKitService.UploadedImage uploaded = imageKitService.upload(file, folder);
        deleteOnRollback(uploaded.fileId());
        if (previous != null) {
            deleteAfterCommit(previous.getImageKitFileId());
        }
        return photoRepository.save(new Photo(file.getOriginalFilename(), file.getContentType(),
                uploaded.url(), uploaded.fileId()));
    }

    void deleteOnRollback(String fileId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteCloudFile(fileId);
                }
            }
        });
    }

    public void deleteAfterCommit(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteCloudFile(fileId);
            }
        });
    }

    private void deleteCloudFile(String fileId) {
        try {
            imageKitService.delete(fileId);
        } catch (RuntimeException ex) {
            log.warn("Could not delete photo from ImageKit (file ID {})", fileId, ex);
        }
    }
}
