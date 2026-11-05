package ca.sheridancollege.restfulhousekeeping.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningSource;

@Repository
public interface CleaningRepository extends JpaRepository<Cleaning,Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select cleaning from Cleaning cleaning where cleaning.id = :id")
	Optional<Cleaning> findByIdForUpdate(Long id);
	
	List<Cleaning> findByManagerIdAndDateTimeStartGreaterThanEqual(
		    Long managerId,
		    LocalDateTime dateTimeStart
		);
	
	List<Cleaning> findByCleanerIdAndDateTimeStartGreaterThanEqual(
		    Long cleanerId,
		    LocalDateTime dateTimeStart
		);
	
	Optional<Cleaning> findFirstByPropertyIdAndDateTimeStartGreaterThanEqualOrderByDateTimeStartAsc(
	    Long propertyId,
	    LocalDateTime dateTimeStart
	);
	
	boolean existsByProperty_IdAndSourceAndExternalEventUid(
			Long propertyId,
			CleaningSource source,
			String externalEventUid
		);

	List<Cleaning> findByCleanerIdAndIsCompleteTrue(Long cleanerId);

}
