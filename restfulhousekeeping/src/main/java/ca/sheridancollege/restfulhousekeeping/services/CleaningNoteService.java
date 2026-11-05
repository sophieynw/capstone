package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningNote;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningNoteResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningNoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class CleaningNoteService {

    private final CleaningRepository cleaningRepository;
    private final CleaningNoteRepository noteRepository;

    @Transactional(readOnly = true)
    public List<CleaningNoteResponse> list(Long cleaningId, User user) {
        requireCleaningAccess(cleaningId, user);
        return noteRepository.findByCleaning_IdOrderByCreatedAtDescIdDesc(cleaningId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CleaningNoteResponse create(Long cleaningId, String content, User user) {
        Cleaning cleaning = requireCleaningAccess(cleaningId, user);
        return toResponse(noteRepository.save(new CleaningNote(cleaning, user, content.strip())));
    }

    private Cleaning requireCleaningAccess(Long cleaningId, User user) {
        Cleaning cleaning = cleaningRepository.findById(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cleaning not found"));

        if (user != null) {
            User participant = switch (user.getRole()) {
                case MANAGER -> cleaning.getManager();
                case CLEANER -> cleaning.getCleaner();
            };
            if (participant != null && participant.getId().equals(user.getId())) {
                return cleaning;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the cleaning's manager and assigned cleaner can access its notes");
    }

    private CleaningNoteResponse toResponse(CleaningNote note) {
        return new CleaningNoteResponse(
                note.getId(),
                note.getAuthor().getFirstName(),
                note.getAuthor().getLastName(),
                note.getContent(),
                note.getCreatedAt()
        );
    }
}
