package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CleaningResponseService {

    private final CleaningRepository cleaningRepository;
    private final UserRepository userRepository;
    private final CleaningChecklistItemResponseService cciResponseService;
    private final PropertyResponseService propertyResponseService;

    public List<CleaningResponse> getMyUpcomingCleanings(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        List<Cleaning> cleanings;

        if (user.getRole() == Role.MANAGER) {
            cleanings = cleaningRepository
                    .findByManagerIdAndDateTimeStartGreaterThanEqual(
                            user.getId(),
                            now
                    );
        } else if (user.getRole() == Role.CLEANER) {
            cleanings = cleaningRepository
                    .findByCleanerIdAndDateTimeStartGreaterThanEqual(
                            user.getId(),
                            now
                    );

        }  else {
            throw new RuntimeException("Invalid user role");    
        }
        
        return cleanings.stream()
        		.map(this::toCleaningResponse)
        		.toList();
        
    }

    public Optional<CleaningResponse> getFirstCleaningByProperty(Long propertyId) {
        return cleaningRepository.findFirstByPropertyIdAndDateTimeStartGreaterThanEqualOrderByDateTimeStartAsc(
        		propertyId,
        		LocalDateTime.now()
        	)
        		.map(this::toCleaningResponse);
    }
    
    @Transactional
    public CleaningResponse completeCleaning(Long cleaningId) {
        Cleaning cleaning = cleaningRepository
            .findByIdForUpdate(cleaningId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cleaning not found"
            ));

        cleaning.markComplete();

        return toCleaningResponse(cleaning);
    }

    @Transactional
    public CleaningResponse updateCleaner(
            Long cleaningId,
            Long cleanerId,
            User manager) {
        if (manager == null || manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers can assign cleaners"
            );
        }

        Cleaning cleaning = cleaningRepository.findByIdForUpdate(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cleaning not found"
                ));

        if (!cleaning.getManager().getId().equals(manager.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This cleaning is not managed by the current manager"
            );
        }

        if (Boolean.TRUE.equals(cleaning.getIsComplete())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A completed cleaning cannot be reassigned"
            );
        }

        User cleaner = null;

        if (cleanerId != null) {
            cleaner = userRepository.findById(cleanerId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cleaner not found"
                    ));

            if (cleaner.getRole() != Role.CLEANER) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The selected user is not a cleaner"
                );
            }

            if (manager.getOrganization() == null
                    || cleaner.getOrganization() == null
                    || !manager.getOrganization().getId()
                            .equals(cleaner.getOrganization().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "The selected cleaner is not on the manager's team"
                );
            }
        }

        cleaning.setCleaner(cleaner);
        return toCleaningResponse(cleaningRepository.save(cleaning));
    }

    @Transactional
    public CleaningResponse updateSchedule(
            Long cleaningId,
            LocalDateTime dateTimeStart,
            LocalDateTime dateTimeEnd,
            User manager) {
        if (manager == null || manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers can update cleaning times"
            );
        }

        Cleaning cleaning = cleaningRepository.findByIdForUpdate(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cleaning not found"
                ));

        if (!cleaning.getManager().getId().equals(manager.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This cleaning is not managed by the current manager"
            );
        }

        if (Boolean.TRUE.equals(cleaning.getIsComplete())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The times for a completed cleaning cannot be changed"
            );
        }

        if (!dateTimeEnd.isAfter(dateTimeStart)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The cleaning end time must be after its start time"
            );
        }

        cleaning.setDateTimeStart(dateTimeStart);
        cleaning.setDateTimeEnd(dateTimeEnd);

        return toCleaningResponse(cleaningRepository.save(cleaning));
    }
    
    public CleaningResponse toCleaningResponse(Cleaning cleaning) {
    	return CleaningResponse.builder()
    			.id(cleaning.getId())
    			.managerId(cleaning.getManager().getId())
    			.cleanerId(
					cleaning.getCleaner() == null
						? null
						: cleaning.getCleaner().getId()
    			)
                .property(propertyResponseService.toCleaningPropertyResponse(
                        cleaning.getProperty()
                ))
    			.dateTimeStart(cleaning.getDateTimeStart())
                .dateTimeEnd(cleaning.getDateTimeEnd())
                .dateTimeStarted(cleaning.getDateTimeStarted())
                .dateTimeCompleted(cleaning.getDateTimeCompleted())
                .notes(cleaning.getNotes())
                .cleaningChecklistItems(
                        cciResponseService
                                .getItemsForCleaning(cleaning.getId())
                )
                .isComplete(cleaning.getIsComplete())
                .build();
    }
    
    
    
}
