package ca.sheridancollege.restfulhousekeeping.beans;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Property {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	@ManyToOne
	@JoinColumn(name="managerId", nullable=false)
	private User manager;
	
	private String name;
	private String street;
	private String unit;
	private String city;
	private String province;
	private String postalCode;
	private String country;
	private String accessInstructions;
	private String propertyPicturePath;
	private String propertyPictureFileId;

	@OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	@Builder.Default
	private List<ChecklistItem> checklistItems = new ArrayList<>();

	@OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	@Builder.Default
	private List<Cleaning> cleanings = new ArrayList<>();

	@OneToOne(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PropertyCalendarIntegration calendarIntegration;

	private LocalTime checkoutTime;
	private LocalTime checkinTime;
}
