package ca.sheridancollege.restfulhousekeeping.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.restfulhousekeeping.beans.Availability;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability,Long> {

}
