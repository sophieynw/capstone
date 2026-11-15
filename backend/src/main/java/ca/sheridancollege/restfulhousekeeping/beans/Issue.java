package ca.sheridancollege.restfulhousekeeping.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "propertyId", nullable = false, foreignKey = @ForeignKey(name = "fk_issue_property"))
    private Property property;

    @ManyToOne
    @JoinColumn(name = "reportedById")
    private User reportedBy;

    private String issueType;
    private String description;
    private LocalDateTime reportedAt;
}