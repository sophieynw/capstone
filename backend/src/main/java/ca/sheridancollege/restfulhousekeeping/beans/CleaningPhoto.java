package ca.sheridancollege.restfulhousekeeping.beans;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CleaningPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaningId")
    private Cleaning cleaning;

    private String fileName;
    private String fileType;

    @Column(length = 2048)
    private String imageKitUrl;
    private String imageKitFileId;

    // Kept for photos saved before ImageKit storage was enabled.
    @Lob
    private byte[] imageData;

    public CleaningPhoto(Cleaning cleaning, String fileName, String fileType,
                         String imageKitUrl, String imageKitFileId) {
        this.cleaning = cleaning;
        this.fileName = fileName;
        this.fileType = fileType;
        this.imageKitUrl = imageKitUrl;
        this.imageKitFileId = imageKitFileId;
    }
}
