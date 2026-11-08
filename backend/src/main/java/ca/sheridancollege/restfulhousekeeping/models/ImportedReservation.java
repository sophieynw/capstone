package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalDate;

public record ImportedReservation(
	    String eventUid,
	    String reservationCode,
	    String reservationUrl,
	    LocalDate checkInDate,
	    LocalDate checkOutDate
) {}
