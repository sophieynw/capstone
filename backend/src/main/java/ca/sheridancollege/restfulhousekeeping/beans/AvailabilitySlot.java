package ca.sheridancollege.restfulhousekeeping.beans;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AvailabilitySlot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne
	@JoinColumn(name="availabilityId")
	private Availability availability;

	@ManyToOne
	@JoinColumn(name="cleanerId", nullable=false)
	private User cleaner;

	@Enumerated(EnumType.STRING)
	private DayOfWeek dayOfWeek;
	LocalTime startTime;
	LocalTime endTime;
}
