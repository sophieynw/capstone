package ca.sheridancollege.restfulhousekeeping.beans;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaningId")
    private Cleaning cleaning;
    private Long uploaderId;

    private String fileName;
    private String fileType;

    @Column(length = 2048)
    private String imageKitUrl;
    private String imageKitFileId;

    // Kept for photos saved before ImageKit storage was enabled.
    @Lob
    private byte[] imageData;

    public Photo(String fileName, String fileType, String imageKitUrl, String imageKitFileId) {
        this(null, null, fileName, fileType, imageKitUrl, imageKitFileId);
    }

    public Photo(Cleaning cleaning, Long uploaderId, String fileName, String fileType,
                 String imageKitUrl, String imageKitFileId) {
        this.cleaning = cleaning;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.imageKitUrl = imageKitUrl;
        this.imageKitFileId = imageKitFileId;
    }
}
