package ca.sheridancollege.restfulhousekeeping.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ca.sheridancollege.restfulhousekeeping.beans.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByCleaning_Property_Manager_IdOrderByReportedAtDesc(Long managerId);
}
