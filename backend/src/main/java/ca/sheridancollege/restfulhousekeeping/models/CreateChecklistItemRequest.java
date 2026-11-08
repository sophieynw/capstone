package ca.sheridancollege.restfulhousekeeping.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChecklistItemRequest {
    private String description;
    private Integer frequencyDays;
}