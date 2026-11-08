package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.CleaningPhoto;
import ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CleaningPhotoRepository extends JpaRepository<CleaningPhoto, Long> {

    @Query("""
            select new ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse(photo.id)
            from CleaningPhoto photo where photo.cleaning.id = :cleaningId order by photo.id
            """)
    List<CleaningPhotoResponse> listByCleaningId(Long cleaningId);

    @Query("""
            select photo.imageKitFileId from CleaningPhoto photo
            where photo.cleaning.id in :cleaningIds and photo.imageKitFileId is not null
            """)
    List<String> findCloudFileIds(List<Long> cleaningIds);

    long countByCleaning_Id(Long cleaningId);

    Optional<CleaningPhoto> findByIdAndCleaning_Id(Long id, Long cleaningId);

    Optional<CleaningPhoto> findFirstByFileName(String fileName);
}
