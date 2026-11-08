package ca.sheridancollege.restfulhousekeeping.beans;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(
		name = "uk_calendar_integration_property",
		columnNames = "propertyId"
	)
)
public class PropertyCalendarIntegration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "propertyId", nullable = false)
	@JsonIgnore
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Property property;

	@Column(nullable = false, length = 2000)
	@JsonIgnore
	private String calendarUrl;

	@Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;

	private LocalDateTime lastSyncAttemptAt;
	private LocalDateTime lastSuccessfulSyncAt;

	@Column(length = 2000)
	private String lastSyncError;
}
