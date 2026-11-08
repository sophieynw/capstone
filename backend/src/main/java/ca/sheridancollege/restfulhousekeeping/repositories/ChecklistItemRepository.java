package ca.sheridancollege.restfulhousekeeping.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
	
	public List<ChecklistItem> findAllByProperty_Id(Long propertyId);

}
