package ca.sheridancollege.restfulhousekeeping.beans;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "cleaning_checklist_item",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"cleaningId", "checklistItemId"}
    )
)
public class CleaningChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cleaningId", nullable = false)
    @JsonBackReference
    private Cleaning cleaning;

    @ManyToOne
    @JoinColumn(name = "checklistItemId")
    private ChecklistItem checklistItem;
    private String customDescription;

    @Builder.Default
    private Boolean isComplete = false;

    @JsonIgnore
    private Boolean automaticallyScheduled;
}
