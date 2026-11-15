package ca.sheridancollege.restfulhousekeeping.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    // Identifies the reporting context so the server can verify access to the property.
    private Long cleaningId;
    private String issueType;
    private String description;
}