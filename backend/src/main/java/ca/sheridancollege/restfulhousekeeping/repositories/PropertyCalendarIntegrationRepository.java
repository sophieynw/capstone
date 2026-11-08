package ca.sheridancollege.restfulhousekeeping.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.restfulhousekeeping.beans.PropertyCalendarIntegration;

@Repository
public interface PropertyCalendarIntegrationRepository
	extends JpaRepository<PropertyCalendarIntegration, Long> {

	Optional<PropertyCalendarIntegration> findByProperty_Id(Long propertyId);

	boolean existsByProperty_IdAndEnabledTrue(Long propertyId);

	List<PropertyCalendarIntegration> findAllByEnabledTrue();
}
