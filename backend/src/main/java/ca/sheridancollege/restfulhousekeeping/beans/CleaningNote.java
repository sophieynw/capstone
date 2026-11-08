package ca.sheridancollege.restfulhousekeeping.beans;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cleaning_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CleaningNote {

    public static final int MAX_CONTENT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cleaning_id", nullable = false)
    private Cleaning cleaning;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public CleaningNote(Cleaning cleaning, User author, String content) {
        this.cleaning = cleaning;
        this.author = author;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
