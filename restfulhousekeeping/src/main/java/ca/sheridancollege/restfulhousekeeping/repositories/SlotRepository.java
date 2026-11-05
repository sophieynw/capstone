package ca.sheridancollege.restfulhousekeeping.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import ca.sheridancollege.restfulhousekeeping.beans.AvailabilitySlot;
import ca.sheridancollege.restfulhousekeeping.beans.DayOfWeek;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<AvailabilitySlot,Long> {
    List<AvailabilitySlot> findByCleanerId(Long cleanerId);
    List<AvailabilitySlot> findByCleanerIdIn(Collection<Long> cleanerIds);

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AvailabilitySlot> findByCleanerIdAndDayOfWeekOrderByStartTime(Long cleanerId, DayOfWeek day);

    Optional<AvailabilitySlot> findByIdAndCleanerId(Long id, Long cleanerId);
}
