package ca.sheridancollege.restfulhousekeeping.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import ca.sheridancollege.restfulhousekeeping.exceptions.CalendarImportException;

@Service
public class CalendarDownloadService {

    private static final int MAX_CALENDAR_SIZE = 2_000_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public byte[] download(String calendarUrl) {
        URI uri = validateUrl(calendarUrl);

        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "text/calendar")
            .GET()
            .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                throw new CalendarImportException(
					HttpStatus.BAD_GATEWAY,
                    "Calendar server returned HTTP "
                        + response.statusCode()
                );
            }

            if (response.body().length > MAX_CALENDAR_SIZE) {
                throw new CalendarImportException(
					HttpStatus.CONTENT_TOO_LARGE,
                    "Calendar file is too large"
                );
            }

            return response.body();
        } catch (IOException exception) {
            throw new CalendarImportException(
				HttpStatus.BAD_GATEWAY,
                "Could not download calendar",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CalendarImportException(
				HttpStatus.BAD_GATEWAY,
                "Calendar download was interrupted",
                exception
            );
        }
    }

    private URI validateUrl(String calendarUrl) {
        final URI uri;

        try {
            uri = URI.create(calendarUrl);
        } catch (IllegalArgumentException exception) {
            throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
                "Invalid calendar URL"
            );
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
                "Calendar URL must use HTTPS"
            );
        }

        String host = uri.getHost();

        if (!isAllowedAirbnbHost(host)) {
            throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
                "Only Airbnb calendar URLs are allowed"
            );
        }

		if (uri.getPath() == null || !uri.getPath().endsWith(".ics")) {
			throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"Airbnb calendar URL must point to an .ics file"
			);
		}

        return uri;
    }

	private boolean isAllowedAirbnbHost(String host) {
		if (host == null) {
			return false;
		}

		String normalizedHost = host.toLowerCase();
		return normalizedHost.equals("airbnb.com")
			|| normalizedHost.endsWith(".airbnb.com")
			|| normalizedHost.equals("airbnb.ca")
			|| normalizedHost.endsWith(".airbnb.ca");
	}
}
