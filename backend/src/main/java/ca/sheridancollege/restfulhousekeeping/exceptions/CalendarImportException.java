package ca.sheridancollege.restfulhousekeeping.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class CalendarImportException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;

	public CalendarImportException(HttpStatusCode status, String reason) {
		super(status, reason);
	}

	public CalendarImportException(HttpStatusCode status, String reason, Throwable cause) {
		super(status, reason, cause);
	}
}
