package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningPhoto;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningPhotoRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class CleaningPhotoService {

    private static final int MAX_PHOTOS = 10;
    private static final byte[] JPEG_SIGNATURE = HexFormat.of().parseHex("ffd8ff");
    private static final byte[] PNG_SIGNATURE = HexFormat.of().parseHex("89504e470d0a1a0a");
    private static final Path LEGACY_UPLOAD_DIR = Path.of("uploads", "photos").toAbsolutePath().normalize();

    private final CleaningRepository cleaningRepository;
    private final CleaningPhotoRepository photoRepository;
    private final ImageKitService imageKitService;

    @Transactional(readOnly = true)
    public List<CleaningPhotoResponse> list(Long cleaningId, User user) {
        requireViewer(findCleaning(cleaningId), user);
        return photoRepository.listByCleaningId(cleaningId);
    }

    @Transactional
    public CleaningPhotoResponse upload(Long cleaningId, MultipartFile file, User user) throws IOException {
        Cleaning cleaning = cleaningRepository.findByIdForUpdate(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning not found"));
        requireCleaner(cleaning, user);

        if (photoRepository.countByCleaning_Id(cleaningId) >= MAX_PHOTOS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cleaning can have up to 10 photos");
        }

        byte[] bytes = file.getBytes();
        MediaType imageType = detectImageType(bytes);
        String extension = MediaType.IMAGE_JPEG.equals(imageType) ? "jpg" : imageType.getSubtype();
        String fileName = UUID.randomUUID() + "." + extension;
        ImageKitService.UploadedImage uploaded = imageKitService.uploadCleaningPhoto(bytes, fileName);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteCloudFile(uploaded.fileId());
                }
            }
        });
        CleaningPhoto photo = photoRepository.save(
                new CleaningPhoto(cleaning, fileName, imageType.toString(), uploaded.url(), uploaded.fileId()));
        return new CleaningPhotoResponse(photo.getId());
    }

    @Transactional(readOnly = true)
    public ImageContent read(Long cleaningId, Long photoId, User user) throws IOException {
        requireViewer(findCleaning(cleaningId), user);
        return imageContent(photoRepository.findByIdAndCleaning_Id(photoId, cleaningId)
                .orElseThrow(CleaningPhotoService::photoNotFound));
    }

    @Transactional(readOnly = true)
    public ImageContent readByFileName(String fileName, User user) throws IOException {
        CleaningPhoto photo = photoRepository.findFirstByFileName(fileName)
                .orElseThrow(CleaningPhotoService::photoNotFound);
        requireViewer(photo.getCleaning(), user);
        return imageContent(photo);
    }

    @Transactional
    public void delete(Long photoId, User user) {
        CleaningPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(CleaningPhotoService::photoNotFound);
        requireCleaner(photo.getCleaning(), user);
        photoRepository.delete(photo);
        if (photo.getImageKitFileId() != null) {
            deleteCloudFilesAfterCommit(List.of(photo.getImageKitFileId()));
        }
    }

    public void deleteCloudPhotosAfterCleaningRemoval(List<Long> cleaningIds) {
        if (!cleaningIds.isEmpty()) {
            deleteCloudFilesAfterCommit(photoRepository.findCloudFileIds(cleaningIds));
        }
    }

    private void deleteCloudFilesAfterCommit(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileIds.forEach(CleaningPhotoService.this::deleteCloudFile);
            }
        });
    }

    private void deleteCloudFile(String fileId) {
        try {
            imageKitService.delete(fileId);
        } catch (RuntimeException ex) {
            log.warn("Could not delete cleaning photo from ImageKit (file ID {})", fileId, ex);
        }
    }

    private Cleaning findCleaning(Long cleaningId) {
        return cleaningRepository.findById(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning not found"));
    }

    private static void requireViewer(Cleaning cleaning, User user) {
        if (cleaning != null && user != null) {
            User participant = switch (user.getRole()) {
                case MANAGER -> cleaning.getManager();
                case CLEANER -> cleaning.getCleaner();
            };
            if (participant != null && participant.getId().equals(user.getId())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the cleaning's manager and assigned cleaner can view its photos");
    }

    private static void requireCleaner(Cleaning cleaning, User user) {
        if (user == null || user.getRole() != Role.CLEANER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned cleaner can change cleaning photos");
        }
        requireViewer(cleaning, user);
    }

    private ImageContent imageContent(CleaningPhoto photo) throws IOException {
        if (photo.getImageKitUrl() != null) {
            byte[] bytes = imageKitService.readCleaningPhoto(photo.getImageKitUrl());
            return new ImageContent(new ByteArrayResource(bytes), MediaType.parseMediaType(photo.getFileType()));
        }

        byte[] bytes = photo.getImageData();
        if (bytes != null) {
            return new ImageContent(new ByteArrayResource(bytes), MediaType.parseMediaType(photo.getFileType()));
        }

        if (photo.getFileName() == null) {
            throw photoNotFound();
        }
        Path path = LEGACY_UPLOAD_DIR.resolve(photo.getFileName()).normalize();
        if (!LEGACY_UPLOAD_DIR.equals(path.getParent()) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw photoNotFound();
        }
        bytes = Files.readAllBytes(path);
        return new ImageContent(new ByteArrayResource(bytes), detectImageType(bytes));
    }

    private static MediaType detectImageType(byte[] bytes) {
        if (startsWith(bytes, JPEG_SIGNATURE)) {
            return MediaType.IMAGE_JPEG;
        }
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return MediaType.IMAGE_PNG;
        }
        if (bytes.length >= 16 && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            return MediaType.valueOf("image/webp");
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Choose a JPEG, PNG, or WebP photo");
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length > signature.length
                && Arrays.equals(bytes, 0, signature.length, signature, 0, signature.length);
    }

    private static ResponseStatusException photoNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
    }

    public record ImageContent(Resource resource, MediaType mediaType) {
    }
}
