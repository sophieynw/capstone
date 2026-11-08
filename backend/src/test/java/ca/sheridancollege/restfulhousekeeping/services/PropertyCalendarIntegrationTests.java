package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.PropertyCalendarIntegration;
import ca.sheridancollege.restfulhousekeeping.models.PropertyResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyCalendarIntegrationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PropertyCalendarIntegrationTests {

	@Autowired
	private PropertyRepository propertyRepository;

	@Autowired
	private PropertyCalendarIntegrationRepository integrationRepository;

	@Autowired
	private PropertyResponseService propertyResponseService;

	@Test
	void reportsConnectionAndDeletesIntegrationWithProperty() {
		Property property = propertyRepository.findById(1L).orElseThrow();
		PropertyCalendarIntegration integration = integrationRepository.saveAndFlush(
			PropertyCalendarIntegration.builder()
				.property(property)
				.calendarUrl("https://www.airbnb.com/test.ics")
				.build()
		);
		property.setCalendarIntegration(integration);

		Long propertyId = property.getId();
		Long managerId = property.getManager().getId();

		PropertyResponse response = propertyResponseService
				.getPropertyById(propertyId, managerId)
				.orElseThrow();

		assertThat(response.getAirbnbCalendarConnected()).isTrue();

		propertyRepository.delete(property);
		propertyRepository.flush();

		assertThat(integrationRepository.existsById(integration.getId())).isFalse();
	}
}
