package ca.sheridancollege.restfulhousekeeping.models;

import ca.sheridancollege.restfulhousekeeping.beans.CleaningNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCleaningNoteRequest(
        @NotBlank @Size(max = CleaningNote.MAX_CONTENT_LENGTH) String content
) {}
