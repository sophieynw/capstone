package ca.sheridancollege.restfulhousekeeping.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ca.sheridancollege.restfulhousekeeping.beans.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByProperty_Manager_IdOrderByReportedAtDesc(Long managerId);

    Optional<Issue> findByIdAndProperty_Manager_Id(Long id, Long managerId);
}
