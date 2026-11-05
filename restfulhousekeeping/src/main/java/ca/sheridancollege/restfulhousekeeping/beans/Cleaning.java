package ca.sheridancollege.restfulhousekeeping.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cleaning_property_source_event",
            columnNames = {"propertyId", "source","externalEventUid"}
        )
    }
)
public class Cleaning {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name="managerId", nullable=false)
	private User manager;
	
	@ManyToOne
	@JoinColumn(name="cleanerId")
	private User cleaner;
	
	@ManyToOne
	@JoinColumn(name="propertyId", nullable=false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Property property;
	
	private LocalDateTime dateTimeStart;
	private LocalDateTime dateTimeEnd;
	private LocalDateTime dateTimeStarted;
	private LocalDateTime dateTimeCompleted;
	private String notes;

	@OneToMany(mappedBy = "cleaning", cascade = CascadeType.REMOVE)
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	@Builder.Default
	private List<CleaningNote> cleaningNotes = new ArrayList<>();

	@OneToMany(mappedBy = "cleaning", cascade = CascadeType.REMOVE)
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	@Builder.Default
	private List<CleaningPhoto> cleaningPhotos = new ArrayList<>();

	
	@OneToMany(
		    mappedBy = "cleaning",
		    cascade = CascadeType.ALL,
		    orphanRemoval = true
		)
	@JsonManagedReference
	@Builder.Default
	private List<CleaningChecklistItem> cleaningChecklistItems = new ArrayList<>();
	private Boolean isComplete;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private CleaningSource source = CleaningSource.MANUAL;

	private String externalEventUid;
	private String externalReservationCode;
	@Column(length = 2000)
	private String reservationUrl;
	private LocalDate importedCheckInDate;
	private LocalDate importedCheckOutDate;

	public void markComplete() {
		if (!Boolean.TRUE.equals(isComplete) || dateTimeCompleted == null) {
			dateTimeCompleted = LocalDateTime.now();
		}
		isComplete = true;
		cleaningChecklistItems.forEach(item -> item.setIsComplete(true));
	}
}
