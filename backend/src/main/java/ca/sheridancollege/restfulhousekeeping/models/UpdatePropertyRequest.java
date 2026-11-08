package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyRequest {
	private Long id;
	private String name;
	private String street;
	private String unit;
	private String city;
	private String province;
	private String postalCode;
	private String country;
	private String accessInstructions;
	private LocalTime checkoutTime;
	private LocalTime checkinTime;
	private Long managerId;
	private List<ChecklistItem> checklistItems = new ArrayList<>();
}
