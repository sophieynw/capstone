package ca.sheridancollege.restfulhousekeeping.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sheridancollege.restfulhousekeeping.models.CalendarImportRequest;
import ca.sheridancollege.restfulhousekeeping.models.CalendarImportResponse;
import ca.sheridancollege.restfulhousekeeping.services.CalendarImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/properties/{propertyId}/calendar-imports")
@SecurityRequirement(name = "Bearer Authentication")
public class CalendarImportController {

	private final CalendarImportService calendarImportService;

	@PostMapping
	public ResponseEntity<CalendarImportResponse> importCalendar(
		@PathVariable Long propertyId,
		@RequestBody CalendarImportRequest request,
		Principal principal
	) {
		CalendarImportResponse response = calendarImportService.importCalendar(
			propertyId,
			request == null ? null : request.icalUrl(),
			principal.getName()
		);

		return ResponseEntity.ok(response);
	}
}
