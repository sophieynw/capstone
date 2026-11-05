package ca.sheridancollege.restfulhousekeeping.services;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.lib.Helper;
import io.imagekit.models.SrcOptions;
import io.imagekit.models.files.FileDeleteParams;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
public class ImageKitService {

    private static final String CLEANING_PICTURES_FOLDER = "/restful-housekeeping/cleaning-pictures";
    private static final String LEGACY_CLEANING_PHOTOS_FOLDER = "/cleaning-photos";

    private final String privateKey;
    private final RestClient deliveryClient;

    public ImageKitService(@Value("${imagekit.private-key:}") String privateKey) {
        this.privateKey = privateKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        deliveryClient = RestClient.builder().requestFactory(factory).build();
    }

    public UploadedImage upload(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "_" +
                (originalName == null || originalName.isBlank() ? "image.jpg" : originalName);

        return upload(file.getBytes(), fileName, folder, false);
    }

    public UploadedImage uploadCleaningPhoto(byte[] bytes, String fileName) {
        return upload(bytes, fileName, CLEANING_PICTURES_FOLDER, true);
    }

    private UploadedImage upload(byte[] bytes, String fileName, String folder, boolean privateFile) {
        FileUploadParams params = FileUploadParams.builder()
                .file(bytes)
                .fileName(fileName)
                .folder(folder)
                .isPrivateFile(privateFile)
                .build();

        FileUploadResponse response = client().files().upload(params);
        String url = response.url().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "ImageKit upload did not return a URL"
        ));
        String fileId = response.fileId().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "ImageKit upload did not return a file ID"
        ));

        return new UploadedImage(url, fileId);
    }

    public byte[] readCleaningPhoto(String url) {
        int folderIndex = url.lastIndexOf(CLEANING_PICTURES_FOLDER + "/");
        if (folderIndex < 0) {
            folderIndex = url.lastIndexOf(LEGACY_CLEANING_PHOTOS_FOLDER + "/");
        }
        if (folderIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid ImageKit photo URL");
        }
        String signedUrl = Helper.create(requirePrivateKey()).buildUrl(SrcOptions.builder()
                .urlEndpoint(url.substring(0, folderIndex))
                .src(url.substring(folderIndex))
                .signed(true)
                .expiresIn(60)
                .build());
        try {
            byte[] bytes = deliveryClient.get().uri(URI.create(signedUrl)).retrieve().body(byte[].class);
            if (bytes != null && bytes.length > 0) {
                return bytes;
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to retrieve photo from ImageKit");
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ImageKit returned an empty photo");
    }

    public void delete(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }

        client().files().delete(FileDeleteParams.builder()
                .fileId(fileId)
                .build());
    }

    private ImageKitClient client() {
        return ImageKitOkHttpClient.builder().privateKey(requirePrivateKey()).build();
    }

    private String requirePrivateKey() {
        if (privateKey == null || privateKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ImageKit is not configured"
            );
        }

        return privateKey;
    }

    public record UploadedImage(String url, String fileId) {
    }
}
