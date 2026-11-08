package ca.sheridancollege.restfulhousekeeping.models;

import ca.sheridancollege.restfulhousekeeping.beans.Issue;
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
    private Long cleaningId;
    private Long propertyId;
    private Long reportedById;
    private String reportedByName;
    private String issueType;
    private String description;
    private LocalDateTime cleaningStart;
    private String propertyName;
    private LocalDateTime reportedAt;

    public static IssueResponse fromIssue(Issue issue) {
        User reporter = issue.getReportedBy();

        return IssueResponse.builder()
                .id(issue.getId())
                .cleaningId(issue.getCleaning() != null ? issue.getCleaning().getId() : null)
                .propertyId(issue.getCleaning() != null && issue.getCleaning().getProperty() != null
                        ? issue.getCleaning().getProperty().getId() : null)
                .reportedById(reporter != null ? reporter.getId() : null)
                .reportedByName(reporter != null
                        ? reporter.getFirstName() + " " + reporter.getLastName()
                        : null)
                .issueType(issue.getIssueType())
                .description(issue.getDescription())
                .cleaningStart(issue.getCleaning() != null
                ? issue.getCleaning().getDateTimeStart() : null)
                .propertyName(issue.getCleaning() != null && issue.getCleaning().getProperty() != null
                ? issue.getCleaning().getProperty().getName() : null)
                .reportedAt(issue.getReportedAt())
                .build();
    }
}
