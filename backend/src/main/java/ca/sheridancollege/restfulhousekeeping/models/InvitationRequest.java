package ca.sheridancollege.restfulhousekeeping.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InvitationRequest(
    @NotBlank @Email String email
) {}
