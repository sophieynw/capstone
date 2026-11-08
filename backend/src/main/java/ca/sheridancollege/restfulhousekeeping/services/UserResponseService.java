package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.models.*;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserResponseService {
    private final UserRepository userRepository;
    private final CleaningRepository cleaningRepository;

    @Transactional
    public Optional<UserResponse> updateUser(Long id, UpdateUserRequest request) {

        return userRepository.findById(id).map(existing -> {

            if (request.getFirstName() != null) {
                existing.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                existing.setLastName(request.getLastName());
            }
            if (request.getEmail() != null) {
                String email = EmailPolicy.normalizeAndValidate(request.getEmail());
                userRepository.findByEmailIgnoreCase(email)
                        .filter(user -> !user.getId().equals(existing.getId()))
                        .ifPresent(user -> {
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Email is already in use.");
                        });
                existing.setEmail(email);
            }
            if (request.getOrganization() != null && existing.getOrganization() == null) {
                existing.setOrganization(request.getOrganization());
            }
            User saved = userRepository.save(existing);
            return toUserResponse(saved);
        });
    }

    @Transactional
    public void removeCleanerFromTeam(Long cleanerId, User manager) {
        if (manager == null || manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers can remove cleaners from a team"
            );
        }

        User cleaner = userRepository.findById(cleanerId)
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
                || !Objects.equals(
                        manager.getOrganization().getId(),
                        cleaner.getOrganization().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The selected cleaner is not on the manager's team"
            );
        }

        cleaningRepository
                .findByCleanerIdAndDateTimeStartGreaterThanEqual(
                        cleanerId,
                        LocalDateTime.now())
                .forEach(cleaning -> cleaning.setCleaner(null));

        cleaner.setOrganization(null);
        userRepository.save(cleaner);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .role(user.getRole())
                .organization(user.getOrganization())
                .build();
    }

}
