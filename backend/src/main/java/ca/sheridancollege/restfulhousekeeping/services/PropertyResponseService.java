package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CreateChecklistItemRequest;
import ca.sheridancollege.restfulhousekeeping.models.PropertyResponse;
import ca.sheridancollege.restfulhousekeeping.models.UpdatePropertyRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyCalendarIntegrationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PropertyResponseService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CleaningChecklistItemResponseService cciResponseService;
    private final ChecklistItemRepository checklistItemRepository;
    private final PropertyCalendarIntegrationRepository calendarIntegrationRepository;

    // find ALL properties for manager
    public List<PropertyResponse> getPropertiesForManager(Long managerId) {
        return propertyRepository.findAllByManager_Id(managerId)
                .stream()
                .map(this::toPropertyResponse)
                .toList();
    }

    // find property by propertyId, scoped to manager's own properties
    public Optional<PropertyResponse> getPropertyById(Long propertyId, Long managerId) {
        return propertyRepository.findByIdAndManager_Id(propertyId, managerId).map(this::toPropertyResponse);
    }

    // CREATE new property record service
    @Transactional
    public PropertyResponse createProperty(Property property, List<CreateChecklistItemRequest> checklistItems) {
        property.setId(null);

        if (property.getManager() == null || property.getManager().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Manager ID is required"
            );
        }

        Long managerId = property.getManager().getId();
        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User " + managerId + " is not a manager");
        }

        property.setManager(manager);
        Property savedProperty = propertyRepository.save(property);

        if (checklistItems != null) {
            List<ChecklistItem> newItems = checklistItems.stream()
                    .map(item -> ChecklistItem.builder()
                            .property(savedProperty)
                            .description(item.getDescription())
                            .frequencyDays(item.getFrequencyDays())
                            .lastCompleted(null)
                            .build())
                    .toList();

            checklistItemRepository.saveAll(newItems);
        }

        return toPropertyResponse(savedProperty);
    }

    // UPDATE existing property record service
    @Transactional
    public Optional<PropertyResponse> updateProperty(Long id, UpdatePropertyRequest request) {

        return propertyRepository.findById(id).map(existing -> {

            if (request.getName() != null) {
                existing.setName(request.getName());
            }
            if (request.getStreet() != null) {
                existing.setStreet(request.getStreet());
            }
            if (request.getUnit() != null) {
                existing.setUnit(request.getUnit());
            }
            if (request.getCity() != null) {
                existing.setCity(request.getCity());
            }
            if (request.getProvince() != null) {
                existing.setProvince(request.getProvince());
            }
            if (request.getPostalCode() != null) {
                existing.setPostalCode(request.getPostalCode());
            }
            if (request.getCountry() != null) {
                existing.setCountry(request.getCountry());
            }
            if (request.getAccessInstructions() != null) {
                existing.setAccessInstructions(request.getAccessInstructions());
            }
            if (request.getCheckoutTime() != null) {
                existing.setCheckoutTime(request.getCheckoutTime());
            }
            if (request.getCheckinTime() != null) {
                existing.setCheckinTime(request.getCheckinTime());
            }
            if (request.getManagerId() != null) {
                User manager = userRepository
                        .findById(request.getManagerId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found: " + request.getManagerId()));

                if (manager.getRole() != Role.MANAGER) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "User " + request.getManagerId() + " is not a manager");
                }

                existing.setManager(manager);
            }

            Property saved = propertyRepository.save(existing);
            return toPropertyResponse(saved);
        });
    }

    private PropertyResponse buildPropertyResponse(
            Property property,
            Boolean airbnbCalendarConnected) {

        return PropertyResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .street(property.getStreet())
                .unit(property.getUnit())
                .city(property.getCity())
                .province(property.getProvince())
                .postalCode(property.getPostalCode())
                .country(property.getCountry())
                .accessInstructions(property.getAccessInstructions())
                .propertyPicturePath(property.getPropertyPicturePath())
                .checkoutTime(property.getCheckoutTime())
                .checkinTime(property.getCheckinTime())
                .airbnbCalendarConnected(airbnbCalendarConnected)
                .build();
    }

    // for fetching property info for managers
    private PropertyResponse toPropertyResponse(Property property) {
        Boolean airbnbCalendarConnected =
                calendarIntegrationRepository.existsByProperty_IdAndEnabledTrue(property.getId());
        return buildPropertyResponse(property, airbnbCalendarConnected);
    }

    // for fetching property info for cleaners, omitting airbnbCalendarConnected info
    public PropertyResponse toCleaningPropertyResponse(Property property) {
        return buildPropertyResponse(property, null);
    }

}
