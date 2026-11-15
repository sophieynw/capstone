package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.Photo;
import ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    @Query("""
            select new ca.sheridancollege.restfulhousekeeping.models.CleaningPhotoResponse(photo.id)
            from Photo photo where photo.cleaning.id = :cleaningId order by photo.id
            """)
    List<CleaningPhotoResponse> listByCleaningId(Long cleaningId);

    @Query("""
            select photo.imageKitFileId from Photo photo
            where photo.cleaning.id in :cleaningIds and photo.imageKitFileId is not null
            """)
    List<String> findCloudFileIds(List<Long> cleaningIds);

    Optional<Photo> findByIdAndCleaningIsNotNull(Long id);

    long countByCleaning_Id(Long cleaningId);

    Optional<Photo> findByIdAndCleaning_Id(Long id, Long cleaningId);

    Optional<Photo> findFirstByFileNameAndCleaningIsNotNull(String fileName);
}
