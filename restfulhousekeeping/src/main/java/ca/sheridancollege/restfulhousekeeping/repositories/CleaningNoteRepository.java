package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.CleaningNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleaningNoteRepository extends JpaRepository<CleaningNote, Long> {

    @EntityGraph(attributePaths = "author")
    List<CleaningNote> findByCleaning_IdOrderByCreatedAtDescIdDesc(Long cleaningId);
}
