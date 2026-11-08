package ca.sheridancollege.restfulhousekeeping.models;

import java.util.List;

import ca.sheridancollege.restfulhousekeeping.beans.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePropertyRequest {
    private Property property;
    private List<CreateChecklistItemRequest> checklistItems;
}