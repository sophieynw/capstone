package ca.sheridancollege.restfulhousekeeping.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningResponse {

	private Long id;
	private Long managerId;
	private Long cleanerId;
	private PropertyResponse property;
	private LocalDateTime dateTimeStart;
	private LocalDateTime dateTimeEnd;
	private LocalDateTime dateTimeStarted;
	private LocalDateTime dateTimeCompleted;
	private String notes;
	private List<CleaningChecklistItemResponse> cleaningChecklistItems;
	private Boolean isComplete;

}
