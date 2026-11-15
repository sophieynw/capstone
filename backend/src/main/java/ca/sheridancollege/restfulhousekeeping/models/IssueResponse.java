package ca.sheridancollege.restfulhousekeeping.models;

import ca.sheridancollege.restfulhousekeeping.beans.Issue;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {

    private Long id;
    private Long propertyId;
    private Long reportedById;
    private String reportedByName;
    private String issueType;
    private String description;
    private String propertyName;
    private LocalDateTime reportedAt;

    public static IssueResponse fromIssue(Issue issue) {
        User reporter = issue.getReportedBy();
        Property property = issue.getProperty();

        return IssueResponse.builder()
                .id(issue.getId())
                .propertyId(property.getId())
                .reportedById(reporter != null ? reporter.getId() : null)
                .reportedByName(reporter != null
                        ? reporter.getFirstName() + " " + reporter.getLastName()
                        : null)
                .issueType(issue.getIssueType())
                .description(issue.getDescription())
                .propertyName(property.getName())
                .reportedAt(issue.getReportedAt())
                .build();
    }
}
