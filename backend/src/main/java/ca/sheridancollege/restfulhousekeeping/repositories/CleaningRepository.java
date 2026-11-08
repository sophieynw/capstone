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

	@Query("""
		select distinct c from Cleaning c left join fetch c.cleaningChecklistItems
		where c.property.id = :propertyId and (c.isComplete is null or c.isComplete = false)
		    and c.dateTimeStart is not null and (c.dateTimeStart >= :now or c.dateTimeEnd >= :now)
		order by c.dateTimeStart, c.id
		""")
	List<Cleaning> findPendingChecklistSchedule(Long propertyId, LocalDateTime now);
	
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
