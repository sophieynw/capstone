package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.Property;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = :propertyId")
    Optional<Property> findByIdForUpdate(Long propertyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = (select c.property.id from Cleaning c where c.id = :cleaningId)")
    Optional<Property> findByCleaningIdForUpdate(Long cleaningId);

    List<Property> findAllByManager_Id(Long managerId);

    Optional<Property> findByIdAndManager_Id(Long propertyId, Long managerId);

}
