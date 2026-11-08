package ca.sheridancollege.restfulhousekeeping.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    private Long cleaningId;
    private String issueType;
    private String description;
}